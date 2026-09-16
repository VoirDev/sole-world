# Security Policy

## Reporting a vulnerability

**Do not open a public issue for a security problem.**

Report it through GitHub's private vulnerability reporting, on the
[Security tab](https://github.com/VoirDev/sole-world/security/advisories/new) of this repository.
That opens a private thread with the maintainers, and it is the only channel that is watched for
this.

A useful report says which version or image tag you tested, how to reproduce it, and what an
attacker gets out of it. A proof of concept helps, but do not let polishing one delay the report.

You should expect an acknowledgement within a few days and an assessment within two weeks. If a fix
is warranted, it ships in the next release and the advisory is published alongside it, crediting you
unless you would rather not be.

## Supported versions

Fixes go into the latest release. There are no maintained release branches: the service is a single
stateless container with no data to migrate, so upgrading is replacing the image tag.

| Version | Supported |
|---------|-----------|
| Latest release | Yes |
| Anything older | No — upgrade |

## What is in scope

This project is an API server and a bundled dataset. In scope:

- Anything that lets a caller reach data or a route the API key model says they cannot — a way past
  `ApiKeyFilter`, a path that escapes the public allowlist, a traversal out of `/assets/`.
- Anything that lets one caller deny service to others — a request that costs far more than the
  GraphQL cost model or the rate limit accounts for.
- Anything that leaks a configured API key, in a response, a log line, or an error document.
- A supply-chain problem in what the image ships: a dependency, a vendored console bundle, or the
  build that produces them.

Out of scope:

- **Data accuracy.** Names, borders, codes and dates can be wrong or out of date; that is an issue
  or a pull request, not a vulnerability. See the Data Notice in the README.
- Findings that only apply because a deployment turned off `API_AUTH_ENABLED`, which is documented
  as a deliberate choice to serve the API publicly.
- Missing hardening on a route that is public by design — `/healthz`, `/assets/`, and the two
  developer consoles while they are switched on.
- Reports from an automated scanner with no demonstrated impact on this service.

## What this service is built to assume

Reading the design before reporting will save you time:

- Every route requires an API key unless [`PublicPaths`](api/src/main/kotlin/dev/voir/sole/world/api/security/PublicPaths.kt)
  says otherwise. The filter is default-deny, so a newly added route is protected until someone opts
  it in.
- Keys are reduced to SHA-256 digests at startup and never held as plain text. A key never appears
  in a log or an error document.
- The dataset is read-only and loaded once. There is no database, no user input that is stored, and
  nothing to write.
- Both developer consoles are served from the image. They fetch nothing from a CDN, and each page
  carries a `Content-Security-Policy` confining it to this origin.
