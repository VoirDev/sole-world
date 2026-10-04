#!/usr/bin/env bash
# Starts a built API image as docker-compose.yml runs it in production (read-only root file system,
# no capabilities, no new privileges, a tmpfs /tmp, consoles off, authentication on) and checks that
# it becomes healthy, enforces its API key, serves data, and carries the expected release version.
#
#   api/smoke-test.sh <image> <expected-version>
#
# The API key is generated for this run and never printed.
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <image> <expected-version>" >&2
  exit 2
fi
image="$1"
expected_version="$2"
name="sole-world-smoke-$$"
port="${SMOKE_TEST_PORT:-18081}"
api_key="$(openssl rand -hex 32)"

cleanup() {
  if [ "$?" -ne 0 ]; then
    docker logs "$name" >&2 || true
  fi
  docker rm --force --volumes "$name" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run --detach --name "$name" \
  --read-only --cap-drop ALL --security-opt no-new-privileges:true \
  --tmpfs /tmp:mode=1777,size=64m --memory 768m \
  --publish "127.0.0.1:$port:8080" \
  --env API_KEYS="$api_key" \
  --env API_AUTH_ENABLED=true \
  --env API_DOCS_ENABLED=false \
  --env GRAPHIQL_ENABLED=false \
  "$image" >/dev/null

base="http://127.0.0.1:$port"
for _ in $(seq 1 90); do
  if curl --silent --fail --output /dev/null "$base/healthz"; then
    break
  fi
  if [ "$(docker inspect --format '{{.State.Running}}' "$name")" != "true" ]; then
    echo "The API exited during start-up." >&2
    exit 1
  fi
  sleep 1
done

status="$(curl --silent --show-error --fail "$base/healthz" | jq -er '.status')"
if [ "$status" != "ok" ]; then
  echo "/healthz reports '$status', expected 'ok'." >&2
  exit 1
fi

# Default-deny: a data route without a key is refused, and with one it answers.
unauthenticated="$(curl --silent --output /dev/null --write-out '%{http_code}' "$base/v1/meta")"
if [ "$unauthenticated" != "401" ]; then
  echo "/v1/meta without an API key answered $unauthenticated, expected 401." >&2
  exit 1
fi
curl --silent --show-error --fail --header "X-API-KEY: $api_key" "$base/v1/meta" | jq -e '.' >/dev/null
curl --silent --show-error --fail --output /dev/null "$base/assets/flags/de_1x1.svg"

# The release version, as the image's label, the jar's manifest and the application's startup log.
label="$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.version"}}' "$image")"
if [ "$label" != "$expected_version" ]; then
  echo "The image's version label is '$label', expected '$expected_version'." >&2
  exit 1
fi
manifest_version="$(docker exec "$name" cat /app/META-INF/MANIFEST.MF | tr -d '\r' |
  sed -n 's/^Implementation-Version: //p')"
if [ "$manifest_version" != "$expected_version" ]; then
  echo "The application jar is version '$manifest_version', expected '$expected_version'." >&2
  exit 1
fi
# Read whole before matching: grep -q stops early, and under pipefail docker logs then fails.
startup_log="$(docker logs "$name" 2>&1)"
if ! grep -qF "Starting ApiApplicationKt v$expected_version using Java" <<<"$startup_log"; then
  echo "The application did not log starting as v$expected_version." >&2
  exit 1
fi

echo "$image serves version $expected_version."
