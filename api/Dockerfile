# syntax=docker/dockerfile:1.7

# ---- Build stage ----
FROM gradle:8.14.3-jdk21 AS build
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
COPY assets assets

RUN --mount=type=cache,target=/home/gradle/.gradle \
    if [ -n "$RELEASE_VERSION" ]; then \
        ./gradlew :api:bootJar -x test -PreleaseVersion="$RELEASE_VERSION" --no-daemon; \
    else \
        ./gradlew :api:bootJar -x test --no-daemon; \
    fi

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api/build/libs/*.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
