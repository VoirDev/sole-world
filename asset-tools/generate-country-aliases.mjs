import fs from 'node:fs/promises';
import path from 'node:path';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';

// Regenerates every country's `aliases` from Unicode CLDR, a Wikidata snapshot and the curation
// file, and reports what would change. Nothing is written without `--write`.
//
//   node generate-country-aliases.mjs              # report the changes
//   node generate-country-aliases.mjs --write      # apply them to data/countries/*/data.json
//   node generate-country-aliases.mjs --refresh    # refetch the Wikidata snapshot first
//
// Wikidata is where most candidates come from and where most of the noise is: nicknames, historic
// names, native-language transliterations, ISO codes. Automatic filters below remove what a rule
// can; the rest was reviewed by hand and recorded in country-aliases/curation.json, so a refreshed
// snapshot only ever surfaces the candidates nobody has looked at yet. Review those in the report,
// add the ones to reject to `drop`, and bump data/meta.json when writing.

const TOOL_DIR = path.dirname(fileURLToPath(import.meta.url));
const ROOT_DIR = path.resolve(TOOL_DIR, '..');
const COUNTRIES_DIR = path.join(ROOT_DIR, 'data/countries');
const LOCALES_FILE = path.join(ROOT_DIR, 'data/locales.json');
const ALIASES_DIR = path.join(TOOL_DIR, 'country-aliases');
const SNAPSHOT_FILE = path.join(ALIASES_DIR, 'wikidata-snapshot.json');
const CURATION_FILE = path.join(ALIASES_DIR, 'curation.json');
const QUERY_FILE = path.join(ALIASES_DIR, 'wikidata.rq');

const require = createRequire(import.meta.url);

/** The base data is English; every other locale is read from data/locales.json. */
const BASE_LOCALE = 'en';

// CLDR's `pt` is Brazilian and `pt-PT` European, while the dataset's `pt` is European. Wikidata
// spells regional variants in lowercase and has several Simplified Chinese label languages.
const CLDR_LOCALES = {'pt': ['pt-PT'], 'pt-BR': ['pt'], 'zh-CN': ['zh']};
const WIKIDATA_LANGUAGES = {'pt-BR': ['pt-br'], 'zh-CN': ['zh-cn', 'zh-hans', 'zh']};

// Items that share a country's ISO code without being the country: a second Antarctica item and
// the Cyprus-at-the-Olympics style duplicates.
const SKIPPED_ITEMS = new Set(['Q10372207', 'Q644636']);

// Where an alias comes from decides where it is listed: the curated sources first, the bulk last.
const SOURCE_ORDER = ['cldr-short', 'cldr-variant', 'curated', 'cldr', 'wikidata-label', 'wikidata-alias'];

/**
 * Folds text the way the API's TextIndex.normalize does, so two aliases the API would treat as one
 * are treated as one here: marks and case dropped, dots and apostrophes removed, other punctuation
 * turned into spaces, and runs of single letters closed up ("U. S. A." is "usa").
 */
function fold(text) {
  const folded = text.normalize('NFKD').replace(/\p{Mn}+/gu, '').toLowerCase().trim();
  const words = folded.replace(/[.'‘’ʼ`]/gu, '').replace(/[\p{P}\s]+/gu, ' ').trim();
  if (!words) {
    return folded;
  }

  const result = [];
  let previousWasInitial = false;
  for (const word of words.split(' ')) {
    const initial = [...word].length === 1 && /\p{L}/u.test(word);
    if (initial && previousWasInitial) {
      result[result.length - 1] += word;
    } else {
      result.push(word);
    }
    previousWasInitial = initial;
  }

  return result.join(' ');
}

async function readJson(file) {
  return JSON.parse(await fs.readFile(file, 'utf8'));
}

async function readCountries() {
  const dirs = (await fs.readdir(COUNTRIES_DIR)).sort();
  return Promise.all(dirs.map(async (dir) => {
    const file = path.join(COUNTRIES_DIR, dir, 'data.json');
    const raw = await fs.readFile(file, 'utf8');
    return {file, raw, data: JSON.parse(raw)};
  }));
}

function cldrTerritories(locale) {
  const json = require(`cldr-localenames-full/main/${locale}/territories.json`);
  return json.main[locale].localeDisplayNames.territories;
}

/** Refetches every country item's labels and aliases, keyed by ISO 3166-1 alpha-2 code. */
async function refreshSnapshot(locales) {
  const languages = locales.flatMap((locale) => WIKIDATA_LANGUAGES[locale] ?? [locale.toLowerCase()]);
  const query = (await fs.readFile(QUERY_FILE, 'utf8'))
    .replace('%LANGUAGES%', languages.map((language) => JSON.stringify(language)).join(', '));

  const url = new URL('https://query.wikidata.org/sparql');
  url.searchParams.set('query', query);
  const response = await fetch(url, {
    headers: {
      'Accept': 'application/sparql-results+json',
      'User-Agent': 'sole-world-country-aliases (https://github.com/VoirDev/sole-world)',
    },
  });
  if (!response.ok) {
    throw new Error(`Wikidata answered ${response.status}: ${await response.text()}`);
  }

  const snapshot = {};
  for (const row of (await response.json()).results.bindings) {
    if (SKIPPED_ITEMS.has(row.item.value.split('/').pop())) {
      continue;
    }
    const entry = ((snapshot[row.iso.value] ??= {})[row.lang.value] ??= {aliases: []});
    if (row.kind.value === 'label') {
      entry.label = row.text.value;
    } else {
      entry.aliases.push(row.text.value);
    }
  }

  // Sorted throughout, so a refresh diffs as the names that changed and nothing else.
  const sorted = {};
  for (const iso of Object.keys(snapshot).sort()) {
    sorted[iso] = {};
    for (const language of Object.keys(snapshot[iso]).sort()) {
      const {label, aliases} = snapshot[iso][language];
      sorted[iso][language] = {...(label ? {label} : {}), aliases: [...new Set(aliases)].sort()};
    }
  }

  await fs.writeFile(SNAPSHOT_FILE, `${JSON.stringify(sorted, null, 1)}\n`);
  console.log(`Refreshed ${path.relative(ROOT_DIR, SNAPSHOT_FILE)}: ${Object.keys(sorted).length} countries.`);
  return sorted;
}

/**
 * Orders Wikidata aliases so that, of two spellings folding to the same text, the properly
 * capitalized and least punctuated one is kept: "St. Barts" over "St barts", "PRC" over "P.R.C.".
 */
function byCareOfSpelling(a, b) {
  const capitals = (text) => (text.match(/\p{Lu}/gu) ?? []).length;
  return capitals(b) - capitals(a) || a.length - b.length || a.localeCompare(b);
}

/** Every candidate for every country and locale, in source order, before any filtering. */
function collectCandidates(countries, locales, snapshot) {
  const territories = new Map();
  const territoriesOf = (locale) => {
    if (!territories.has(locale)) {
      territories.set(locale, cldrTerritories(locale));
    }
    return territories.get(locale);
  };

  const candidates = new Map();
  for (const {data: country} of countries) {
    const byLocale = new Map();
    for (const locale of [BASE_LOCALE, ...locales]) {
      const name = locale === BASE_LOCALE
        ? country.name
        : country.translations.find((translation) => translation.locale === locale)?.name;
      if (name === undefined) {
        continue;
      }

      const seen = new Set([fold(name)]);
      const list = [];
      const add = (text, source) => {
        const trimmed = locale === BASE_LOCALE ? text.trim().replace(/^the\s+/i, '') : text.trim();
        const folded = fold(trimmed);
        if (folded && !seen.has(folded)) {
          seen.add(folded);
          list.push({text: trimmed, source});
        }
      };

      for (const cldrLocale of CLDR_LOCALES[locale] ?? [locale]) {
        const names = territoriesOf(cldrLocale);
        if (names[country.iso2]) {
          add(names[country.iso2], 'cldr');
        }
        for (const alt of ['short', 'variant']) {
          if (names[`${country.iso2}-alt-${alt}`]) {
            add(names[`${country.iso2}-alt-${alt}`], `cldr-${alt}`);
          }
        }
      }
      for (const language of WIKIDATA_LANGUAGES[locale] ?? [locale]) {
        const entry = snapshot[country.iso2]?.[language];
        if (entry?.label) {
          add(entry.label, 'wikidata-label');
        }
        for (const alias of [...(entry?.aliases ?? [])].sort(byCareOfSpelling)) {
          add(alias, 'wikidata-alias');
        }
      }

      byLocale.set(locale, {name, list});
    }
    candidates.set(country.iso2, byLocale);
  }

  return candidates;
}

/**
 * Applies the rules that need no judgement: a flag emoji or anything else without a letter is not
 * a name, an ISO code is already searchable, a name another country also answers to is ambiguous,
 * and a Wikidata alias in brackets, with digits, lowercase or
 * of essay length is a disambiguator, a code or a description rather than a name.
 */
function filterCandidates(countries, candidates) {
  const owners = new Map();
  const own = (text, iso) => {
    const folded = fold(text);
    if (!owners.has(folded)) {
      owners.set(folded, new Set());
    }
    owners.get(folded).add(iso);
  };
  const displayNames = [];
  for (const [iso, byLocale] of candidates) {
    for (const {name, list} of byLocale.values()) {
      own(name, iso);
      list.forEach((candidate) => own(candidate.text, iso));
      displayNames.push([iso, ` ${fold(name)} `]);
    }
  }

  const codes = new Map(countries.map(({data}) => [data.iso2, new Set([data.iso2, data.iso3, data.numericCode].map(fold))]));
  for (const [iso, byLocale] of candidates) {
    for (const entry of byLocale.values()) {
      entry.list = entry.list.filter(({text, source}) => {
        const folded = fold(text);
        const fromWikidata = source.startsWith('wikidata');
        return /\p{L}/u.test(text)
          && !codes.get(iso).has(folded)
          && ![...owners.get(folded)].some((owner) => owner !== iso)
          && !displayNames.some(([owner, name]) => owner !== iso && name.includes(` ${folded} `))
          && !(fromWikidata && /[()[\]/\d]|\p{Extended_Pictographic}/u.test(text))
          && !(fromWikidata && /^\p{Ll}/u.test(text))
          && !(fromWikidata && text.length > 60);
      });
    }
  }
}

/** Applies the curation file and settles each list: ordered by source, one entry per folded form. */
function curate(candidates, curation) {
  const result = new Map();
  for (const [iso, byLocale] of candidates) {
    const aliases = new Map();
    for (const [locale, {name, list}] of byLocale) {
      // Matched folded, so a rejection holds for every spelling of the name: dropping "VSA" also
      // drops "V. S. A.", whichever of the two the snapshot happens to offer first.
      const dropped = new Set(Object.keys(curation.drop[iso]?.[locale] ?? {}).map(fold));
      const kept = list.filter(({text}) => !dropped.has(fold(text)));
      for (const text of curation.add[iso]?.[locale] ?? []) {
        kept.push({text, source: 'curated'});
      }
      kept.sort((a, b) => SOURCE_ORDER.indexOf(a.source) - SOURCE_ORDER.indexOf(b.source));

      const seen = new Set([fold(name)]);
      aliases.set(locale, kept.filter(({text}) => !seen.has(fold(text)) && seen.add(fold(text))).map(({text}) => text));
    }
    result.set(iso, aliases);
  }

  // A curated addition can still collide with another country; DatasetIntegrity would refuse it.
  const owners = new Map();
  for (const [iso, aliases] of result) {
    for (const [locale, list] of aliases) {
      for (const text of [candidates.get(iso).get(locale).name, ...list]) {
        const folded = fold(text);
        owners.set(folded, (owners.get(folded) ?? new Set()).add(iso));
      }
    }
  }
  const ambiguous = [];
  for (const [iso, aliases] of result) {
    for (const [locale, list] of aliases) {
      for (const text of list) {
        const others = [...owners.get(fold(text))].filter((owner) => owner !== iso);
        if (others.length > 0) {
          ambiguous.push(`${iso} ${locale} "${text}" also names ${others.join('/')}`);
        }
      }
    }
  }
  if (ambiguous.length > 0) {
    throw new Error(`Aliases naming more than one country:\n  ${ambiguous.join('\n  ')}`);
  }

  return result;
}

/** Rewrites a country file with its aliases, keeping every other key where it was. */
function withAliases(country, aliases) {
  const result = {};
  for (const [key, value] of Object.entries(country)) {
    if (key === 'aliases') {
      continue;
    }
    result[key] = value;
    if (key === 'name') {
      result.aliases = aliases.get(BASE_LOCALE) ?? [];
    }
  }
  result.translations = country.translations.map(({aliases: _, ...translation}) => {
    const list = aliases.get(translation.locale) ?? [];
    return list.length > 0 ? {...translation, aliases: list} : translation;
  });

  return result;
}

async function main() {
  const args = new Set(process.argv.slice(2));
  const locales = (await readJson(LOCALES_FILE)).map((locale) => locale.id);
  const countries = await readCountries();

  const snapshot = args.has('--refresh') ? await refreshSnapshot(locales) : await readJson(SNAPSHOT_FILE);
  const curation = await readJson(CURATION_FILE);

  const candidates = collectCandidates(countries, locales, snapshot);
  filterCandidates(countries, candidates);
  const aliases = curate(candidates, curation);

  let total = 0;
  const changes = [];
  const writes = [];
  for (const {file, raw, data} of countries) {
    const current = new Map([[BASE_LOCALE, data.aliases ?? []], ...data.translations.map((t) => [t.locale, t.aliases ?? []])]);
    const settled = new Map();
    for (const [locale, generated] of aliases.get(data.iso2)) {
      // What the dataset already lists keeps its place and its spelling, so regenerating does not
      // reshuffle reviewed lists or swap "Союз" for "союз"; only real additions are appended.
      const before = current.get(locale) ?? [];
      const generatedFolds = new Set(generated.map(fold));
      const beforeFolds = new Set(before.map(fold));
      const kept = before.filter((text) => generatedFolds.has(fold(text)));
      const added = generated.filter((text) => !beforeFolds.has(fold(text)));
      const removed = before.filter((text) => !generatedFolds.has(fold(text)));
      settled.set(locale, [...kept, ...added]);
      total += kept.length + added.length;

      if (added.length > 0 || removed.length > 0) {
        changes.push(`${data.iso2} ${locale}: ${[...added.map((t) => `+${t}`), ...removed.map((t) => `-${t}`)].join(', ')}`);
      }
    }

    const updated = `${JSON.stringify(withAliases(data, settled), null, 2)}${raw.endsWith('\n') ? '\n' : ''}`;
    if (updated !== raw) {
      writes.push([file, updated]);
    }
  }

  console.log(`${total} aliases across ${countries.length} countries.`);
  if (changes.length > 0) {
    console.log(`Changes against the dataset:\n  ${changes.join('\n  ')}`);
  }
  if (writes.length === 0) {
    console.log('The dataset is up to date.');
  } else if (args.has('--write')) {
    await Promise.all(writes.map(([file, content]) => fs.writeFile(file, content)));
    console.log(`Wrote ${writes.length} country files. Bump data/meta.json.`);
  } else {
    console.log(`${writes.length} country files would change. Run with --write to apply them.`);
  }
}

await main();
