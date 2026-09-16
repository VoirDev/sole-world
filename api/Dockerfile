# syntax=docker/dockerfile:1.7

# ---- Build stage ----
# Pinned to the build machine's own architecture: the only thing this stage produces is a jar, which
# is the same bytes on every platform, so building it once natively beats building it again under
# emulation for each architecture the image is published for.
FROM --platform=$BUILDPLATFORM gradle:9.7.1-jdk21 AS build
WORKDIR /workspace
ARG RELEASE_VERSION

COPY gradle gradle
COPY gradlew gradlew
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY api/build.gradle.kts api/build.gradle.kts

RUN --mount=type=cache,target=/home/gradle/.gradle \
    if [ -n "$RELEASE_VERSION" ]; then \
        ./gradlew :api:dependencies -PreleaseVersion="$RELEASE_VERSION" --no-daemon; \
    else \
        ./gradlew :api:dependencies --no-daemon; \
    fi || true

COPY api api
COPY data data

RUN --mount=type=cache,target=/home/gradle/.gradle \
    if [ -n "$RELEASE_VERSION" ]; then \
        ./gradlew :api:bootJar -x test -PreleaseVersion="$RELEASE_VERSION" --no-daemon; \
    else \
        ./gradlew :api:bootJar -x test --no-daemon; \
    fi

# ---- Layer stage ----
# Splits the application jar into its Spring Boot layers, so that the dependencies -- which change
# only when a version does -- are a separate Docker layer from this project's own classes.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jre-alpine AS layers
WORKDIR /jar
COPY --from=build /workspace/api/build/libs/api.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination /layers

# ---- Runtime stage ----
# No --platform, so this is built once per architecture the image is published for. It is a JRE plus
# the application, which is why publishing for both amd64 and arm64 costs almost nothing.
FROM eclipse-temurin:25-jre-alpine

# The service reads its own files and writes logs to stdout. It never needs to write to disk, own a
# port below 1024, or do anything else root is for, so it does not run as root.
RUN addgroup -S -g 10001 sole && adduser -S -u 10001 -G sole -H -s /sbin/nologin sole

WORKDIR /app

# Layers are ordered least to most likely to change, because a layer invalidates every layer after
# it. The 9,773 image files are 90 MB that does not change between releases, so they go first: put
# inside the jar, as they used to be, a one-line code change reshipped all of it.
# No --chmod: it applies one mode to files and directories alike, and a directory without its
# execute bit cannot be traversed, so the assets would be unreadable. The source permissions are
# already read-only for this user, which is all that is wanted.
COPY --chown=root:root assets /app/assets
COPY --from=layers --chown=root:root /layers/dependencies/ ./
COPY --from=layers --chown=root:root /layers/spring-boot-loader/ ./
COPY --from=layers --chown=root:root /layers/snapshot-dependencies/ ./
COPY --from=layers --chown=root:root /layers/dataset/ ./
COPY --from=layers --chown=root:root /layers/application/ ./

USER 10001:10001

# Sized from measurement on the bundled dataset: the live heap settles around 110 MB, reaching about
# 135 MB if callers exercise all sixteen translation languages, and metaspace commits about 66 MB.
#
# MaxRAMPercentage is 50 rather than 75 because the heap is not the only thing inside the limit:
# metaspace, the code cache and worker-thread stacks all come out of the same budget, and a JVM that
# sizes its heap to three quarters of the limit leaves too little for them. The container was
# OOM-killed rather than degrading.
#
# The metaspace ceiling and ExitOnOutOfMemoryError exist to make a failure legible: a bounded
# metaspace reports what ran out, and exiting turns a heap-exhausted JVM that would otherwise thrash
# into a fast restart under `restart: unless-stopped`.
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC \
    -XX:InitialRAMPercentage=25 \
    -XX:MaxRAMPercentage=50 \
    -XX:MaxMetaspaceSize=192m \
    -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Declared on the image so it describes itself outside Compose, for anything that runs it directly.
HEALTHCHECK --interval=10s --timeout=5s --start-period=20s --retries=5 \
    CMD wget -q -O- http://localhost:8080/healthz || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
