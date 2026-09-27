# Contributing

Thanks for helping. This project is world reference data plus a small read-only API over it, and by
far the most valuable contribution is a data correction.

## Correcting or extending the data

The dataset lives in [`data/`](data) and is the source of truth — it is compiled into the image at
build time, so a correction here is a release, not a migration.

1. **Find the file.** Countries, with their states and cities nested inside them, are under
   `data/countries/<name>/data.json`. Everything else is a flat file named after the resource:
   `currencies.json`, `cryptos.json`, `languages.json`, `locales.json`, `regions.json`,
   `timezones.json`, `central_banks.json`, `flags.json`, `media_assets.json`.
2. **Keep the shape.** Field names are camelCase; ids are stable and must stay unique; a reference
   to another record must name one that exists, spelled exactly as that record spells its id.
   An id is the record's public standard code where one exists — ISO 3166-1 alpha-2 for a
   country, ISO 4217 for a currency, ISO 639-1 for a language, the IANA name for a timezone, the
   country and state codes joined by a hyphen for a state — and a lowercase alias otherwise, such
   as `federal-reserve`. An alias is the name people actually use: a central bank's is its common
   English name with the country's short name (`central-bank-of-iran`, not
   `central-bank-of-islamic-republic-of-iran`), or its native name where it is known by that
   (`deutsche-bundesbank`, `banco-de-la-republica`). Cities keep their numbers. A translation's
   `locale` is the id of a locale in `locales.json`, spelled exactly; the API serves exactly the
   locales listed there. Never change an id that has been released:
   clients store them. All of this is checked at startup by
   [`DatasetIntegrity`](api/src/main/kotlin/dev/voir/sole/world/api/dataset/DatasetIntegrity.kt), so
   `./gradlew :api:test` will tell you if something does not line up.
3. **Say where it came from.** A correction is much easier to accept with a source: an official
   government or central-bank page, an ISO registry, a UN or IANA listing. "This is what it is
   called locally" is a fine source too — say so.
4. **Bump the dataset version.** `data/meta.json` carries a version and a date. Changing the data
   without changing the version leaves every cached `ETag` claiming a response is still current when
   it is not.

Nothing else needs touching for a data change. No code, no schema, no contract.

## Adding or changing images

Image files live in [`assets/`](assets) and their metadata is **generated**, not hand-edited.

1. Add the SVG to `assets/flags/` or `assets/cryptos/`, named `<key>_1x1.svg` for a square image or
   `<key>_4x3.svg` for a wide one. The key is what the record that owns the image refers to.
2. Run the generator, which renders every raster size and rewrites `data/media_assets.json`:

   ```bash
   cd asset-tools
   npm install
   npm run generate:assets
   ```

3. Point the owning record at the new asset ids — a `flags.json` entry's `square` and `wide`, or a
   coin's `logoId` — and re-run the generator so the description picks up the owner's name. An
   asset's id is derived from its file, so it is known before the generator runs: `flag-<key>-square`
   and `flag-<key>-wide` for a flag, `crypto-<key>-square` for a coin's logo.

## Changing the API

Both transports are contract-first. The contract is the source, the code implements it:

- REST: [`api/src/main/resources/openapi/openapi.yaml`](api/src/main/resources/openapi/openapi.yaml)
  generates the controller interfaces and models.
- GraphQL: [`api/src/main/resources/schema/schema.graphqls`](api/src/main/resources/schema/schema.graphqls)
  generates the types.

Edit the contract first, then make the code satisfy it. A controller cannot drift from what is
published, because it implements the generated interface.

**Both transports, or neither.** Every capability a resource has is reachable from both — see "The
two transports offer the same things" in the README. A change that adds a filter to a REST
collection adds it to the GraphQL list field too.

## Running it

```bash
./gradlew build              # tests and the linter, which is what CI runs
./gradlew :api:test          # just the suite
./gradlew ktlintFormat       # fix what the linter can fix
./gradlew :api:bootRun       # locally, with API_KEYS set
docker compose up            # the way it is deployed
```

Formatting is ktlint on the `intellij_idea` style, which is what IntelliJ produces by default. The
wrapping rules that disagree with how this codebase is written are switched off in
[`.editorconfig`](.editorconfig), with the reasoning beside them — if the linter tells you to
reformat something that already reads well, that is worth raising rather than working around.

`API_KEYS` must be set to something at least 16 characters, or the application refuses to start.
Copy `.env.example` to `.env` first.

## What a good pull request looks like

- **One thing.** A data correction and an API change are two pull requests.
- **Tested.** A behaviour change comes with a test that fails without it. The suite runs in seconds.
- **Explained.** The commit message says what was wrong and why this is the fix, not what the diff
  already shows.
- **In keeping.** Match the surrounding code: KDoc on public declarations, comments that say why
  rather than what, and no trailing whitespace.

## Reporting a security problem

Do not open a public issue. See [SECURITY.md](SECURITY.md).
