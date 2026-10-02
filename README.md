# Sole World

Open-source world reference data service with GraphQL and REST APIs for countries, regions,
subregions, states, cities, currencies, languages, timezones, central banks, flags, and related
media assets.

Sole World is built for teams that need a self-hosted, queryable source of country and location
metadata. It runs as a **single container with no database**: the curated dataset ships inside the
image and is loaded into memory at startup, so `docker compose up` is the whole deployment.

## What This Project Provides

- GraphQL API for world, country, geography, language, currency, cryptocurrency, timezone, central
  bank, flag, and media asset data.
- REST API with predefined relationship includes, described by a checked-in OpenAPI 3.1 contract.
- No database, no migrations and no volumes: the bundled dataset is read into memory at startup.
- Relevance search that tolerates typos, accents and scripts, across every supported language.
- API keys supplied through a single environment variable.
- Docker image build support and a one-service Docker Compose setup, published for `linux/amd64` and
  `linux/arm64`.
- Apache 2.0 licensed source code.

## Data Notice

World data changes over time and can be difficult to keep perfectly accurate. Some records may be
incomplete, outdated, or inaccurate.

This project is provided as-is, without warranties about data correctness, completeness, or fitness
for a specific use case. Corrections, additions, and updates are welcome through issues and pull
requests.

## Tech Stack

- Kotlin
- Spring Boot
- Netflix DGS GraphQL (schema-first)
- OpenAPI 3.1 with OpenAPI Generator (spec-first)
- Gradle
- Docker and Docker Compose

## Repository Structure

```text
api/           Spring Boot application serving both APIs
assets/        Static media assets served by the API
asset-tools/   Scripts for generating media asset metadata
data/          Source JSON datasets loaded into memory at startup
gradle/        Gradle wrapper and version catalog
VERSION        The release version, read by the build
```

Inside `api/`, code is organized by feature. Each feature owns its in-memory read model, its GraphQL
fetchers and its REST controller:

```text
dataset/       Loads the bundled JSON and provides shared indexing, search and pagination
security/      API key verification
rest/          Include handling, problem responses, caching, OpenAPI publication
country/ currency/ language/ region/ subregion/ centralbank/ state/ city/ timezone/ flag/ mediaasset/
```

## API Endpoints

When running with the default Docker Compose configuration:

- API base URL: `http://localhost:18080`
- REST API: `http://localhost:18080/v1`
- REST reference: `http://localhost:18080/docs`
- OpenAPI contract: `http://localhost:18080/openapi.yaml`
- GraphQL endpoint: `http://localhost:18080/graphql`
- GraphiQL explorer: `http://localhost:18080/graphiql`
- Health: `http://localhost:18080/healthz`

`/healthz` and `/assets/**` are always public. `/docs`, `/openapi.yaml` and `/graphiql` are public
while their consoles are switched on — see [Developer Consoles](#developer-consoles). Everything
else needs an API key.

## Authentication

Set `API_KEYS` on the container to one or more keys, separated by commas or whitespace:

```bash
API_KEYS="$(openssl rand -hex 32)"
```

Send a key with every request, as either header:

```text
X-API-KEY: <key>
Authorization: Bearer <key>
```

Keys are hashed with SHA-256 at startup and never held in memory as plain text. Each key must be at
least 16 characters or the application refuses to start. There is no user account, admin UI or
database of clients: rotating a key means changing the environment variable and restarting.

To run a deliberately public deployment, set `API_AUTH_ENABLED=false`. The application logs a
warning at startup when it does.

### Rate limiting

Each key carries a request allowance on `/v1/**` and `/graphql`: `API_RATE_LIMIT_BURST` requests it
may spend back to back, refilling at `API_RATE_LIMIT_REQUESTS_PER_MINUTE`. Spending it answers `429`
with a `Retry-After` header. `GET /v1/meta` reports the allowance, so a client can pace itself
instead of discovering the limit by hitting it.

Allowances are held in memory and counted per replica — replicas are independent by design, and a
shared counter would reintroduce the external dependency this service exists to avoid. With
authentication off there is no key to attribute a request to, so the caller's address stands in.
`/healthz` and `/assets/**` are never limited: an orchestrator must be able to reach the health
check however busy the service is.

## REST API

Every resource follows the same shape, so learning one endpoint teaches the rest.

| Resource | Item | Sub-resources |
| --- | --- | --- |
| `/v1/countries` | `/{id}` | `/states` `/cities` `/currencies` `/languages` `/timezones` `/central-banks` |
| `/v1/currencies` | `/{id}` | `/countries` `/central-banks` |
| `/v1/cryptos` | `/{id}` | |
| `/v1/languages` | `/{id}` | `/countries` |
| `/v1/locales` | `/{id}` | |
| `/v1/regions` | `/{id}` | `/subregions` `/countries` |
| `/v1/subregions` | `/{id}` | `/countries` |
| `/v1/central-banks` | `/{id}` | `/countries` `/currencies` |
| `/v1/states` | `/{id}` | `/cities` |
| `/v1/cities` | `/{id}` | |
| `/v1/timezones` | `/{id}` | |
| `/v1/flags` | `/{id}` | |
| `/v1/media-assets` | `/{id}` | |
| `/v1/meta` | | dataset version, supported locales, limits |

`{id}` is the record's identifier, described [below](#identifiers). A few resources also resolve
other standard codes: a country its alpha-3 and numeric codes, a currency its ISO 4217 numeric
code, a coin its ticker.

```bash
curl -H "X-API-KEY: $API_KEY" http://localhost:18080/v1/countries/US
curl -H "X-API-KEY: $API_KEY" http://localhost:18080/v1/currencies/EUR
curl -H "X-API-KEY: $API_KEY" http://localhost:18080/v1/languages/fr
curl -H "X-API-KEY: $API_KEY" http://localhost:18080/v1/states/US-CA
curl -H "X-API-KEY: $API_KEY" http://localhost:18080/v1/timezones/Europe/Paris
```

### Assets

Every media asset carries two names. `key` is the machine key — `zw` for Zimbabwe's flag, `btc` for
Bitcoin's logo — and it is also the name the files are stored under, so it is what a client matches
on. It is unique per aspect ratio, so a flag's square and wide images share one. `description` is
prose naming what the asset shows, for people rather than for matching.

Image paths are published rooted at this deployment — `/assets/flags/us_1x1.svg` — so a client can
use them as-is against the API's own origin, with nothing to concatenate. `/assets/` needs no API
key and is cached for a year, because the files are immutable for the life of the image.

The files sit beside the jar rather than inside it, in their own Docker layer, and are read from
`assets/` in the working directory. That folder only matters while `ASSET_BASE_URL` is empty; with it
set, no published URL points at `/assets/`, so a deployment can leave the files out of the image
entirely.

Set `ASSET_BASE_URL` to front them with a CDN, and every path becomes an absolute URL against that
origin instead — in the media asset endpoints, in a flag's renditions, in a coin's logo, on both
transports. The CDN holds `flags/` and `cryptos/` at its root, as the
[R2 bucket](assets/README.md#the-bucket) does, so the `/assets/` route is not part of those URLs:
`https://cdn.example.com/flags/us_1x1.svg`.

### Currencies

`/v1/currencies` covers 155 currencies, active and obsolete, with their ISO 4217 codes, symbols,
minor-unit precision and the flag of the territory that issues them.

Each one carries `popularity`, a 0–100 score of how widely it is used today. The scale blends
foreign-exchange turnover, payment share and reserve status at the top — the dollar is 100, the euro
96, the yen 92 — with the size of the issuing economy and the number of countries using it further
down. Obsolete currencies score 0.

**Currencies are listed by popularity rather than by name**, because an alphabetical first page of
them runs from the Afghan afghani to the Bahraini dinar without reaching either the dollar or the
euro. `sort` picks another ordering and `order` reverses it:

```bash
# the dollar, the euro, the yen, ...
curl -H "X-API-KEY: $API_KEY" http://localhost:18080/v1/currencies

# sort: popularity (default), name, code, year
curl -H "X-API-KEY: $API_KEY" 'http://localhost:18080/v1/currencies?sort=name'
curl -H "X-API-KEY: $API_KEY" 'http://localhost:18080/v1/currencies?sort=year&order=desc'
```

`sort` defaults to `popularity`, and `order` to the direction the chosen field is useful in:
descending for `popularity`, ascending for `name`, `code` and `year`. An unrecognized value returns
`400` listing the ones that exist. Searching orders by relevance instead, with popularity breaking
ties — `?query=dollar` opens on the US dollar — and an explicit `sort` reorders those matches
without widening them.

On GraphQL the same two arguments are enums: `currencies(sort: NAME, order: DESC)`.

### Cryptocurrencies

`/v1/cryptos` covers 150 coins with their tickers, launch years, minor-unit precision and, for 100 of
them, a bundled logo under `/assets/cryptos/`.

A coin is identified by its alias (`/v1/cryptos/btc`) and also resolves by its ticker. The two
differ on purpose: a ticker is what a coin trades under and can be reassigned, while the alias is the
stable lowercase key, and it is also the name the coin's logo files are stored under. `include=logo`
embeds that logo the same way `include=flag` embeds a country's.

Coin names are not translated — a coin is called the same thing everywhere — so `lang` on this
resource only selects the collation order.

### Identifiers

A record's `id` is the public standard code for it wherever one exists, and a Sole World alias where
none does. It is what a client should store: `US` rather than a number that only means something to
this service.

| Resource | `id` | Example |
| --- | --- | --- |
| Country | ISO 3166-1 alpha-2 code | `US` |
| Currency | ISO 4217 alpha code | `EUR` |
| Language | ISO 639-1 code | `fr` |
| Locale | BCP 47 language tag | `pt-BR` |
| Timezone | IANA timezone name | `Europe/Paris` |
| State | country alpha-2 code and state code, joined by a hyphen | `US-CA` |
| Cryptocurrency | alias | `btc` |
| Region, subregion | alias | `europe`, `western-europe` |
| Central bank | alias | `federal-reserve`, `bank-of-england` |
| Flag | alias; a territory's flag is its lowercase alpha-2 code | `us`, `eu` |
| Media asset | alias: owner kind, key and shape | `flag-us-square` |
| City | number | `120784` |

- **Every `id` is a string except a city's.** Cities have no public code, so they keep their numbers
  rather than names that would change when a city is renamed.
- **Identifiers match regardless of case.** `/v1/countries/us` finds `US`, and a response always
  spells an id the way the table does.
- **A state with no code** gets an alias of the same form, made from its name: `AX-MARIEHAMN`.
- **A timezone id keeps its slashes in the path**: `/v1/timezones/America/Argentina/Buenos_Aires`.
- **Only `id` is used to refer to another record** — `regionId`, `currencyIds`, `stateId` and every
  other reference hold ids, never alternative codes.

### Includes

`include` embeds related records in one response, so simple cases need one request instead of
several. This is deliberately less efficient than a GraphQL query that asks for exactly what it
needs — it buys simplicity, and the cost is bounded.

```bash
curl -H "X-API-KEY: $API_KEY" \
  'http://localhost:18080/v1/countries/US?include=region,currencies,languages,flag'
```

| Resource | Accepted `include` values |
| --- | --- |
| Country | `region` `subregion` `flag` `currencies` `languages` `timezones` `centralBanks` `states` `aliases` |
| Currency | `flag` `countries` `centralBanks` `replacedBy` |
| Language | `flag` `countries` |
| Locale | `language` |
| Region | `subregions` `countries` |
| Subregion | `region` `countries` |
| CentralBank | `countries` `currencies` |
| State | `country` `cities` |
| City | `state` `country` |
| Flag | `squareAsset` `wideAsset` |

Rules:

- Includes are **one level deep**. An embedded record never carries its own includes.
- Included collections are capped at 50 records. When one is cut, its name appears in
  `truncatedIncludes` and its own endpoint pages through the rest.
- `aliases` on a country adds a field rather than a record: the other names it is known by, in the
  response language — `USA` and `United States of America`, or `США` with `?lang=ru`.
- A country's cities are **not** includable — some countries have more than 19,000. Use
  `/v1/countries/{id}/cities`.
- An unrecognized value returns `400` listing the values that endpoint does accept.

### Paging, filtering and search

Collections take `page` (zero-based) and `size` (max 50), and return `{ "items": [...], "page": {...} }`.

`query` on a collection runs a relevance-ranked search that tolerates typos, missing accents and a
different script from the stored name, and that matches a country by any of its names in any
language — `USA`, `Czech Republic` and `Бирма` find what you would expect. On a sub-resource it is a literal name filter, so the
reported total stays exact.

**Every collection accepts it**, including flags — where it ranks over captions and emoji, so
pasting in a flag emoji is how you find out which flag it is. Every collection is also ordered by
the name a reader of the requested language would expect, flags and coins included, even though
their own names are the same in every language. Currencies are the one exception: they are ordered
by `popularity` and take `sort` and `order`, as described above.

```bash
# finds Germany
curl -H "X-API-KEY: $API_KEY" 'http://localhost:18080/v1/countries?query=Germny'
curl -H "X-API-KEY: $API_KEY" 'http://localhost:18080/v1/countries?query=Германия'

# filters combine
curl -H "X-API-KEY: $API_KEY" 'http://localhost:18080/v1/countries?regionId=europe&currencyId=EUR'

# which flag is this?
curl -H "X-API-KEY: $API_KEY" --get --data-urlencode 'query=🇿🇼' \
  'http://localhost:18080/v1/flags'
```

### Errors and caching

Failures are RFC 9457 problem documents (`application/problem+json`). Successful responses carry a
strong `ETag`, `Cache-Control` and `Vary: Accept-Language`; because the dataset is fixed for the life
of a deployment, a conditional `If-None-Match` request always answers `304` while the deployment
stands.

The validator is a pure function of the dataset version, the URL and the language that URL resolves
to — none of which requires reading the dataset — so a conditional request is **answered before the
endpoint runs**. Revalidating a 50-item page of cities costs a hash, around a millisecond, against
the 300 ms the first `200` for that language costs.

How widely a response may be cached follows the auth model. While `API_AUTH_ENABLED` is on,
responses are `private`: the data is not confidential, but a shared cache or CDN that stored a body
fetched with a valid key and served it to callers presenting none would make the key unenforceable
at the edge. A deployment running with authentication off marks them `public`. Static assets under
`/assets/` are public either way — they need no key to fetch.

## GraphQL API

Each resource has three root fields, named for what they do: `countries(page:, query:, ...)` lists
or searches, `country(id:)` loads one, and `countriesByIds(ids:)` loads a batch of at most 50.
Paginated fields use optional `PageInput` with zero-based pages and a maximum size of 50. Heavy nested
relationships, such as `Country.states`, `Country.cities` and `State.cities`, also use `PageInput`,
accept an optional name `query`, and return paginated page objects.

### The two transports offer the same things

**Every capability a resource has is reachable from both.** Concretely:

- each resource has a list field carrying the same filters and search as its REST collection —
  `cities(query:, countryId:, stateId:)` matches `/v1/cities?query=&countryId=&stateId=`;
- each single-item field takes the same identifier the REST path segment takes, so
  `country(id: "US")` and `GET /v1/countries/US` resolve identically;
- every relationship REST offers as an `include` or a sub-resource is a field on the GraphQL type.

Two things are deliberately one-sided, because each is that transport's answer to the same problem —
avoiding a round trip per related record — and neither has anything to say on the other:

| | REST | GraphQL |
| --- | --- | --- |
| Fetch related records with the parent | `include=region,currencies` | select the fields |
| Fetch many records by id at once | one request each, or the collection | `countriesByIds(ids: [...])` |

There is no `isValidX` field. A nullable lookup answers the same question for every resource —
`country(id: "XX") { id }` is null when it does not exist — and it worked for eleven resources
where `isValidX` only ever covered six.

### Query limits

A document is rejected before any resolver runs if it nests deeper than
`GRAPHQL_MAX_QUERY_DEPTH` or costs more than `GRAPHQL_MAX_QUERY_COMPLEXITY`.

Cost is not a field count. Counting fields makes aliasing free, which is the wrong answer for this
service: twenty aliased `cities(page: { size: 1 })` selections name almost nothing and ask for
twenty scans of the largest entity in the dataset. Each field instead carries a weight, declared in
the schema with `@cost` and equal to the record set the field may scan in thousands, and a document's
cost is the sum of the weights it selects. A by-id lookup repeated twenty times stays cheap; twenty
city listings do not.

## Localization

Translated fields are selected from the standard `Accept-Language` request header. If the header is
missing, asks for English, or cannot be resolved to a supported translation locale, the API returns
the base English data.

Translations are held per locale, a BCP 47 tag declared in `data/locales.json`. `GET /v1/locales`
(or the `locales` GraphQL query) lists them with their names in the requested language and in their
own, which is what a language picker needs. The bundled locales:

| Locale  | Language              |
|---------|-----------------------|
| `ko`    | Korean                |
| `pt-BR` | Portuguese (Brazil)   |
| `pt`    | Portuguese            |
| `nl`    | Dutch                 |
| `hr`    | Croatian              |
| `fa`    | Persian               |
| `de`    | German                |
| `es`    | Spanish               |
| `fr`    | French                |
| `ja`    | Japanese              |
| `it`    | Italian               |
| `zh-CN` | Chinese (Simplified)  |
| `tr`    | Turkish               |
| `ru`    | Russian               |
| `uk`    | Ukrainian             |
| `pl`    | Polish                |

You can request a language manually by sending the header with your GraphQL request:

```bash
curl http://localhost:18080/graphql \
  -H 'Content-Type: application/json' \
  -H "X-API-KEY: $API_KEY" \
  -H 'Accept-Language: ru' \
  -d '{"query":"{ countriesByIds(ids: [\"US\", \"DE\"]) { id name region { name } currencies { iso3 name } } }"}'
```

A tag that names no locale falls back to a locale of the same language, preferring the one spelled
as the bare language: `Accept-Language: de-DE` resolves to `de`, `pt-PT` to `pt`, and `zh-TW` to
`zh-CN`, while `pt-BR,pt;q=0.8` resolves to `pt-BR`. English headers such as `en` or `en-US` use the
base English data.

Every successful response names the locale it was written in with `Content-Language`, or `en` for the
base data, so a client can tell a translation from a fallback.

## Quick Start With Docker Compose

One service, no database, no volumes. Start from the template:

```bash
cp .env.example .env
```

Put a real key in it:

```bash
openssl rand -hex 32
```

Then:

```bash
docker compose up
```

Compose reads `.env` automatically, and `.env` is gitignored so real keys never reach the
repository. Compose also refuses to start without `API_KEYS`, so a deployment cannot accidentally
come up unauthenticated — set `API_AUTH_ENABLED=false` if that is genuinely what you want.

Without a `.env` file you can pass everything inline:

```bash
API_KEYS="$(openssl rand -hex 32)" docker compose up
```

Run in the background, and stop:

```bash
docker compose up -d
docker compose down
```

Once it is up:

| | |
| --- | --- |
| Health | <http://localhost:18080/healthz> |
| REST reference | <http://localhost:18080/docs> |
| OpenAPI contract | <http://localhost:18080/openapi.yaml> |
| GraphiQL | <http://localhost:18080/graphiql> |

## Build a Docker Image Locally

Build from the repository root — the Dockerfile compiles the application and bakes in the dataset
and media assets, so nothing else is needed:

```bash
docker build -f api/Dockerfile -t sole-world-api:local .
```

The application takes its version from the [`VERSION`](VERSION) file, records it in the jar's
manifest and logs it at startup. To stamp a different version into a one-off build:

```bash
docker build \
  -f api/Dockerfile \
  --build-arg RELEASE_VERSION=1.0.0 \
  -t sole-world-api:1.0.0 \
  .
```

Point Compose at the image you just built by setting `API_IMAGE` in `.env`:

```env
API_IMAGE=sole-world-api:local
```

or inline for a single run:

```bash
API_IMAGE=sole-world-api:local API_KEYS="$(openssl rand -hex 32)" docker compose up
```

To run the image directly, without Compose:

```bash
docker run --rm -p 18080:8080 \
  -e API_KEYS="$(openssl rand -hex 32)" \
  sole-world-api:local
```

The image runs as an unprivileged user (uid 10001) and carries its own `HEALTHCHECK`, so it
describes itself to anything that runs it. Compose adds the rest of the confinement — see
[Production Deployment](#production-deployment).

## Developer Consoles

Two consoles ship with the service, and each has its own switch:

| Variable | Serves | Default |
|---------------------|-----------------------------------------------|---------|
| `API_DOCS_ENABLED`  | `/docs` and `/openapi.yaml`                    | `true`  |
| `GRAPHIQL_ENABLED`  | `/graphiql`                                    | `true`  |

Both are readable without an API key while they are on, because a caller cannot reasonably be asked
to authenticate before being allowed to read what the API offers.

Switching one off **removes it**. The routes stop existing, and they also leave the set of paths
served without a key — so an unauthenticated request gets `401` rather than a `404` that would
confirm the deployment could have served it. The APIs themselves are unaffected.

```bash
API_DOCS_ENABLED=false GRAPHIQL_ENABLED=false docker compose up -d
```

### Both consoles are served by the image

Neither console fetches anything from a CDN. The reference renderer and GraphiQL are downloaded at
build time, verified against the SHA-256 digests pinned in
[`api/console-assets.txt`](api/console-assets.txt), and served from this origin — so a console runs
no code this project did not choose, and **both work with no outbound internet access**, which is
the same promise the bundled dataset makes. Each page also carries a `Content-Security-Policy`
confining it to this origin, which is what stops a future renderer version from quietly reaching for
a third party.

Upgrading a console means editing that file: change the version in the URL, run
`curl -sSLf <url> | shasum -a 256`, and replace the digest. If an upstream file changes without the
digest changing with it, the build fails rather than shipping.

This is why GraphiQL is pinned to the 4.x line. Version 5 is published only as an ES module graph
meant to be resolved from a CDN at request time; 4.x publishes a self-contained bundle whose only
externals are React and ReactDOM. The console that the GraphQL starter ships is switched off in
favour of this one.

The OpenAPI document is also checked into the repository at
`api/src/main/resources/openapi/openapi.yaml`, so consumers can generate clients from it even
against a deployment that does not publish it.

### What the published image carries

Every released image is built for `linux/amd64` and `linux/arm64`, and each architecture carries two
attestations: an SPDX **SBOM** of everything inside it, and **SLSA provenance** recording what was
built, from which commit, by which workflow run. The image is built once, on the release pull
request, and the bytes that were tested there are the bytes that are published. The manifest list
is signed with cosign keyless signing, so the signature is bound to the one workflow that publishes
releases, on `main`, rather than to a key someone has to be told to trust:

```bash
cosign verify ghcr.io/voirdev/sole-world-api:<version> \
  --certificate-identity https://github.com/VoirDev/sole-world/.github/workflows/publish-release.yml@refs/heads/main \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com
```

Releases up to 1.0.4 were signed by the former `release.yml` workflow; verify those with
`--certificate-identity-regexp '^https://github\.com/VoirDev/sole-world/\.github/workflows/release\.yml@'`
in place of `--certificate-identity`.

A release is gated on the image carrying no known **critical** vulnerability that has a fix
available, on either architecture. Lower severities are reported in the workflow log rather than
blocking: base-image advisories appear and are fixed continuously, and a release process that stops
because one landed that morning is one that gets bypassed.

## Production Deployment

Copy the template and edit it on the server:

```bash
cp .env.example .env
```

A production `.env` differs from the local one in four places — a strong key, consoles off, real
origins, and a pinned image:

```env
API_IMAGE=ghcr.io/voirdev/sole-world-api:1.0.1
API_PORT=8080

# openssl rand -hex 32, one per consumer
API_KEYS=<generated-key>,<another-generated-key>
API_AUTH_ENABLED=true

API_DOCS_ENABLED=false
GRAPHIQL_ENABLED=false

CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com

API_MEMORY_LIMIT=768M
LOG_LEVEL=INFO
```

Then:

```bash
docker compose up -d
```

If you prefer not to keep a `.env` on the host, the same values work as environment variables, which
suits a secret manager or an orchestrator's own configuration:

```bash
API_IMAGE=ghcr.io/voirdev/sole-world-api:1.0.1 \
API_PORT=8080 \
API_KEYS="$(openssl rand -hex 32)" \
API_DOCS_ENABLED=false \
GRAPHIQL_ENABLED=false \
CORS_ALLOWED_ORIGINS=https://app.example.com \
docker compose up -d
```

Checklist:

- [ ] Put the API behind a reverse proxy with HTTPS. The service speaks plain HTTP.
- [ ] Generate long random keys, one per consumer, so a single key can be revoked alone.
- [ ] Keep `API_KEYS` out of the image and out of version control.
- [ ] Turn both consoles off unless you intend the contract to be public.
- [ ] Replace the `*` CORS origin with your real frontends.
- [ ] Pin `API_IMAGE` to a version tag rather than `latest`.
- [ ] Point your health check at `/healthz`; it reports the dataset version that is loaded.
- [ ] Set a request timeout on the reverse proxy in front of the service. The rate limit bounds how
      often a caller may ask; a timeout bounds how long any one request may take.

### Response headers

Every response, including a rejected one, carries `X-Content-Type-Options: nosniff` and
`Referrer-Policy: no-referrer`. The two console pages add a `Content-Security-Policy` confining them
to this origin.

`nosniff` is the one that earns its place. Problem documents echo part of the request back — an
unknown `include` value is named so you can see what you got wrong, and the request URI appears in
`instance` — which is safe exactly as long as a browser treats the response as the
`application/problem+json` it says it is.

### Container confinement

The service reads its own classpath and writes logs to stdout. It never writes to disk, never owns a
privileged port, and never starts another process — so the image and the Compose service say so, and
a flaw in the application has nothing to escalate into:

| Setting | Effect |
|---------|--------|
| `USER 10001:10001` (image)     | The JVM does not run as root |
| `read_only: true`              | The root filesystem cannot be written |
| `tmpfs: /tmp`                  | The one scratch directory the JVM wants, in memory, capped at 64 MB |
| `cap_drop: [ALL]`              | No Linux capabilities at all |
| `no-new-privileges:true`       | No setuid binary can raise privileges |
| `pids: 256`                    | A fork bomb cannot take the host down with it |
| `cpus`, `memory`               | One container cannot starve its neighbours |

`API_CPU_LIMIT` and `API_PIDS_LIMIT` tune the last two if your host needs different numbers.

Rotating a key means editing `API_KEYS` and restarting. List the old and new key together while
consumers migrate, then drop the old one. Because every replica is self-contained, scaling out is
just running more containers.

### Resources

The dataset is held in memory. On the bundled data — 250 countries, 5,069 states, 150,375 cities —
the **live heap settles at roughly 110 MB** after a full collection, and the service starts in about
two seconds. The Compose file sets a 768 MB limit.

Listing a resource is ordered by the name the caller sees, so each entity keeps one ordering per
translation language. Orderings are built the first time a language is asked for and then reused, so
the first list request in a given language is slower than the rest — on the order of 300 ms for
cities, a millisecond or two afterwards. A deployment only pays for the languages its callers
actually use; exercising all sixteen adds roughly **25 MB**, taking the live heap to about 135 MB.

The limit covers more than the heap. Alongside it the JVM commits about 66 MB of metaspace, tens of
megabytes of compiled code, and one stack per worker thread. So the container gives the heap half the
limit rather than three quarters (`-XX:MaxRAMPercentage=50`) and caps the worker pool at 64
(`SERVER_THREADS_MAX`) — every request reads an in-memory index and is done in about a millisecond,
so a larger pool buys no throughput and costs stacks out of the same budget.

Measured on the bundled data, with all sixteen languages exercised and 600 requests including 200
concurrent fuzzy searches, a 768 MB container settles at about **590 MB, 77% of its limit**.

Reported container memory will always look higher than the live heap, because the JVM uses the heap
it was given before collecting, so usage tracks the limit rather than the working set. Lowering
`API_MEMORY_LIMIT` lowers both. Raise it if you extend the dataset substantially.

A metaspace ceiling and `-XX:+ExitOnOutOfMemoryError` are set so that running out is legible: the
JVM reports what it ran out of and exits, and `restart: unless-stopped` brings it back — rather than
thrashing, or being OOM-killed by the kernel with nothing to read afterwards.

### What a release actually ships

The image is built in layers ordered least to most likely to change, so a release only transfers
what differs from the one before it:

| Layer | Size | Changes when |
|-------|------|--------------|
| Image assets | 95 MB | a flag or logo is added or redrawn |
| Dependencies | 43 MB | a dependency version moves |
| Spring Boot loader | 0.4 MB | Spring Boot moves |
| Bundled dataset | 38 MB | the data is corrected or extended |
| **Application** | **7.8 MB** | **any code change** |

The assets used to be copied onto the classpath and the dataset shared a layer with the classes, so
the whole 130 MB jar was one layer: a one-line change reshipped all of it, including 90 MB of images
that had not moved in months. The application jar is now 48 MB, and a code-only release pulls 7.8 MB.

Updating the data means releasing a new image. There is nothing to migrate and no volume to back up.

## Useful Environment Variables

All of these are listed with comments in [`.env.example`](.env.example).

| Variable                       | Purpose                                                | Default                                 |
|--------------------------------|--------------------------------------------------------|-----------------------------------------|
| `API_KEYS`                     | Accepted API keys, comma or whitespace separated        | none; **required**                      |
| `API_AUTH_ENABLED`             | Require an API key at all                               | `true`                                  |
| `API_DOCS_ENABLED`             | Serve `/docs` and `/openapi.yaml`                       | `true`                                  |
| `GRAPHIQL_ENABLED`             | Serve `/graphiql`                                       | `true`                                  |
| `API_IMAGE`                    | Docker image used by Compose                            | `ghcr.io/voirdev/sole-world-api:latest` |
| `API_PORT`                     | Host port mapped to the API container                   | `18080`                                 |
| `API_MEMORY_LIMIT`             | Container memory limit                                  | `768M`                                  |
| `SERVER_THREADS_MAX`           | Maximum worker threads                                  | `64`                                    |
| `API_CPU_LIMIT`                | Container CPU limit                                     | `2`                                     |
| `API_PIDS_LIMIT`               | Maximum processes in the container                      | `256`                                   |
| `CORS_ALLOWED_ORIGINS`         | Allowed CORS origins                                    | `*`                                     |
| `ASSET_BASE_URL`               | Origin asset paths are published under                  | this deployment                         |
| `GRAPHQL_MAX_QUERY_DEPTH`      | Maximum GraphQL query depth                             | `30`                                    |
| `GRAPHQL_MAX_QUERY_COMPLEXITY` | Maximum GraphQL query cost                              | `1000`                                  |
| `API_RATE_LIMIT_ENABLED`       | Rate limit `/v1/**` and `/graphql` per key              | `true`                                  |
| `API_RATE_LIMIT_REQUESTS_PER_MINUTE` | Sustained requests allowed per key                | `600`                                   |
| `API_RATE_LIMIT_BURST`         | Requests per key allowed back to back                   | `120`                                   |
| `LOG_LEVEL`                    | Root log level                                          | `INFO`                                  |

## Example REST Request

```bash
curl -H "X-API-KEY: $API_KEY" \
  'http://localhost:18080/v1/currencies/EUR?include=countries,centralBanks'
```

Ask for translated display fields with `Accept-Language`, or pin the language into the URL so the
response stays cacheable on its own:

```bash
curl -H "X-API-KEY: $API_KEY" -H 'Accept-Language: fr' \
  'http://localhost:18080/v1/countries/US'

curl -H "X-API-KEY: $API_KEY" \
  'http://localhost:18080/v1/countries/US?lang=fr'
```

## Example GraphQL Query

Call GraphQL with the `X-API-KEY` header:

```bash
curl http://localhost:18080/graphql \
  -H 'Content-Type: application/json' \
  -H "X-API-KEY: $API_KEY" \
  -d '{"query":"{ countriesByIds(ids: [\"US\", \"DE\"]) { id name iso2 iso3 currencies { iso3 name } languages { code name } } }"}'
```

Add `Accept-Language` to receive translated display fields:

```bash
curl http://localhost:18080/graphql \
  -H 'Content-Type: application/json' \
  -H "X-API-KEY: $API_KEY" \
  -H 'Accept-Language: fr' \
  -d '{"query":"{ countriesByIds(ids: [\"US\", \"DE\"]) { id name iso2 iso3 currencies { iso3 name } languages { code name } } }"}'
```

## Development

Run tests:

```bash
./gradlew test
```

Build the API jar:

```bash
./gradlew :api:bootJar
```

The project uses Java 21. The Gradle wrapper is included, so a local Gradle installation is not
required. Tests need no Docker and no database: they load a small fixture dataset from
`api/src/test/resources/test-dataset`.

### Both APIs are contract-first

Neither API's shape is inferred from the code:

- GraphQL is generated from `api/src/main/resources/schema/schema.graphqls` by DGS Codegen.
- REST is generated from `api/src/main/resources/openapi/openapi.yaml` by OpenAPI Generator.

Controllers implement the generated interfaces, so a change to a published contract that the code
does not follow is a compile error rather than a runtime surprise. Edit the schema or the spec
first, then the code.

### The dataset is checked at startup

Every cross-reference in the bundled data is verified when it is loaded — a flag's media assets, a
currency's flag and successor, a coin's logo, a country's region, subregion, flag, currencies,
timezones and languages, a central bank's countries and currencies, every translation's locale and
each locale's language — along with the uniqueness of every id. Anything that does not resolve fails startup with a message naming the rule and the rows,
reporting everything it found at once so correcting the data is one pass.

The dataset is immutable and read once, so this costs nothing per request and turns what used to be
a silently absent relationship at request time into a build failure. It found the bundled data
shipping two different languages under id 53, one of which could not be loaded by id at all.

### Changing the data

The dataset lives in `data/` and is copied into the image at build time. Bump `version` in
`data/meta.json` when it changes; the loaded version is reported by `/healthz` and `/v1/meta`.

## Contributing

Contributions are welcome, especially:

- Data corrections and source-backed updates.
- Missing translations, identifiers, flags, or media assets.
- API improvements, GraphQL schema refinements, and OpenAPI contract refinements.
- Documentation, deployment, and testing improvements.

[CONTRIBUTING.md](CONTRIBUTING.md) covers how the data is laid out, how image metadata is generated,
and how a contract change works on both transports. A data correction usually touches one JSON file
and nothing else.

Found a security problem? Do not open a public issue — [SECURITY.md](SECURITY.md) has the private
reporting channel and what is in scope.

Each release's notes are generated from the commits since the previous tag, on the
[Releases page](https://github.com/VoirDev/sole-world/releases). How a release is made is in
[CONTRIBUTING.md](CONTRIBUTING.md#releasing).

## License

This project is licensed under the Apache License 2.0. See `LICENSE` for details.
