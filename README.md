# Sole World

Open-source world reference data service with a GraphQL API for countries, regions, subregions,
states, cities, currencies, languages, timezones, central banks, flags, and related media assets.

Sole World is built for teams that need a self-hosted, queryable source of country and location
metadata. The project packages curated JSON data, generated media assets, a PostgreSQL-backed Spring
Boot API, GraphQL schema, Flyway migrations, and Docker deployment files.

## What This Project Provides

- GraphQL API for world, country, geography, language, currency, timezone, central bank, flag, and
  media asset data.
- PostgreSQL persistence with Flyway database migrations.
- Automatic data import from the bundled `data/` and `assets/` directories.
- Admin UI for managing API clients and access keys.
- Docker image build support and Docker Compose setup for local or server deployment.
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
- Netflix DGS GraphQL
- PostgreSQL
- Flyway
- Gradle
- Docker and Docker Compose

## Repository Structure

```text
api/           Spring Boot GraphQL API
assets/        Static media assets served by the API
asset-tools/   Scripts for generating media asset metadata
data/          Source JSON datasets imported by the API
gradle/        Gradle wrapper and version catalog
```

## API Endpoints

When running with the default Docker Compose configuration:

- API base URL: `http://localhost:18080`
- GraphQL endpoint: `http://localhost:18080/graphql`
- GraphiQL explorer: `http://localhost:18080/graphiql`
- Admin UI: `http://localhost:18080/admin`
- PostgreSQL: `localhost:15432`

GraphQL requests use the `X-API-KEY` header. Create and manage client API keys through the admin UI.
Plural lookup fields require an `ids` argument and accept at most 50 ids per request.
Paginated listing fields use optional `PageInput` with zero-based pages and a maximum size of 50.
Heavy nested country relationships, such as `Country.states` and `Country.cities`, also use
`PageInput`, accept an optional name `query`, and return paginated page objects.

## Localization

Translated fields are selected from the standard `Accept-Language` request header. If the header is
missing, asks for English, or cannot be resolved to a supported translation language, the API returns
the base English data.

Supported translation languages:

| Code    | Language              |
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
  -H 'X-API-KEY: your-api-key' \
  -H 'Accept-Language: ru' \
  -d '{"query":"{ countries(ids: [840, 124]) { id name region { name } currencies { iso3 name } } }"}'
```

Region variants fall back to the base supported language when available. For example,
`Accept-Language: de-DE` resolves to `de`, and `Accept-Language: pt-BR,pt;q=0.8` resolves to
`pt-BR`. English headers such as `en` or `en-US` use the base English data.

## Quick Start With Docker Compose

Run the published image and a local PostgreSQL database:

```bash
docker compose up
```

Run in the background:

```bash
docker compose up -d
```

Stop the stack while keeping the database volume:

```bash
docker compose down
```

Remove the database volume too:

```bash
docker compose down -v
```

Default local credentials are intentionally simple:

- Admin username: `admin`
- Admin password: `admin`
- Database name: `sole_world`
- Database username: `sole_world`
- Database password: `sole_world`

Change these values before exposing the service outside your machine.

## Build a Docker Image Locally

Build the API image from the repository root:

```bash
docker build -f api/Dockerfile -t sole-world-api:local .
```

Optionally pass a release version into the Gradle build:

```bash
docker build \
  -f api/Dockerfile \
  --build-arg RELEASE_VERSION=1.0.0 \
  -t sole-world-api:1.0.0 \
  .
```

Run the local image with Docker Compose:

```bash
API_IMAGE=sole-world-api:local docker compose up
```

## Local Configuration

The compose file reads configuration from environment variables. You can set them inline:

```bash
API_IMAGE=sole-world-api:local \
API_PORT=18081 \
POSTGRES_PORT=15433 \
SUPER_ADMIN_USERNAME=admin \
SUPER_ADMIN_PASSWORD=change-me \
ACCESS_KEY_HASH_SECRET=change-this-secret \
docker compose up
```

Or create a local `.env` file:

```env
API_IMAGE=sole-world-api:local
API_PORT=18080
POSTGRES_PORT=15432

POSTGRES_DB=sole_world
POSTGRES_USER=sole_world
POSTGRES_PASSWORD=change-me

SUPER_ADMIN_USERNAME=admin
SUPER_ADMIN_PASSWORD=change-me
ACCESS_KEY_HASH_SECRET=change-this-secret
```

The `.env` file is read automatically by Docker Compose.

## Production Deployment With Docker Compose

On a production server, use a published image or your own locally built image, set strong secrets,
and run Compose in detached mode:

```bash
API_IMAGE=ghcr.io/voirdev/sole-world-api:latest \
API_PORT=8080 \
POSTGRES_PORT=5432 \
POSTGRES_DB=sole_world \
POSTGRES_USER=sole_world \
POSTGRES_PASSWORD=replace-with-a-strong-password \
SUPER_ADMIN_USERNAME=admin \
SUPER_ADMIN_PASSWORD=replace-with-a-strong-password \
ACCESS_KEY_HASH_SECRET=replace-with-a-long-random-secret \
DGS_GRAPHIQL_ENABLED=false \
docker compose up -d
```

Recommended production practices:

- Put the API behind a reverse proxy with HTTPS.
- Do not use the default admin password, database password, or access-key hash secret.
- Restrict database access to trusted hosts only.
- Set `DGS_GRAPHIQL_ENABLED=false` if you do not want to expose GraphiQL.
- Configure `CORS_ALLOWED_ORIGINS` for your actual frontend origins.
- Back up the PostgreSQL volume regularly.
- Pin `API_IMAGE` to a version tag instead of `latest` for repeatable deployments.

## Useful Environment Variables

| Variable                       | Purpose                                  | Default                                 |
|--------------------------------|------------------------------------------|-----------------------------------------|
| `API_IMAGE`                    | Docker image used by Compose             | `ghcr.io/voirdev/sole-world-api:latest` |
| `API_PORT`                     | Host port mapped to the API container    | `18080`                                 |
| `POSTGRES_PORT`                | Host port mapped to PostgreSQL           | `15432`                                 |
| `POSTGRES_DB`                  | PostgreSQL database name                 | `sole_world`                            |
| `POSTGRES_USER`                | PostgreSQL username                      | `sole_world`                            |
| `POSTGRES_PASSWORD`            | PostgreSQL password                      | `sole_world`                            |
| `SUPER_ADMIN_USERNAME`         | Admin UI username                        | `admin`                                 |
| `SUPER_ADMIN_PASSWORD`         | Admin UI password                        | `admin`                                 |
| `ACCESS_KEY_HASH_SECRET`       | Secret used when hashing client API keys | `local-dev-access-key-secret`           |
| `DGS_GRAPHIQL_ENABLED`         | Enable or disable GraphiQL               | `true`                                  |
| `CORS_ALLOWED_ORIGINS`         | Allowed CORS origins                     | `*`                                     |
| `SEED_IMPORT_ENABLED`          | Import bundled seed data on startup      | `true`                                  |
| `GRAPHQL_MAX_QUERY_DEPTH`      | Maximum GraphQL query depth              | `8`                                     |
| `GRAPHQL_MAX_QUERY_COMPLEXITY` | Maximum GraphQL query complexity         | `250`                                   |

## Example GraphQL Query

After creating an API key in the admin UI, call GraphQL with the `X-API-KEY` header:

```bash
curl http://localhost:18080/graphql \
  -H 'Content-Type: application/json' \
  -H 'X-API-KEY: your-api-key' \
  -d '{"query":"{ countries(ids: [840, 124]) { id name iso2 iso3 currencies { iso3 name } languages { code name } } }"}'
```

Add `Accept-Language` to receive translated display fields:

```bash
curl http://localhost:18080/graphql \
  -H 'Content-Type: application/json' \
  -H 'X-API-KEY: your-api-key' \
  -H 'Accept-Language: fr' \
  -d '{"query":"{ countries(ids: [840, 124]) { id name iso2 iso3 currencies { iso3 name } languages { code name } } }"}'
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
required.

## Contributing

Contributions are welcome, especially:

- Data corrections and source-backed updates.
- Missing translations, identifiers, flags, or media assets.
- API improvements and GraphQL schema refinements.
- Documentation, deployment, and testing improvements.

Please open an issue or pull request with enough context to verify the proposed change.

## License

This project is licensed under the Apache License 2.0. See `LICENSE` for details.
