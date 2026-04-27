# ─────────────────────────────────────────────────────────────────
# Stage 1 – Build
# Uses a full JDK image to compile and assemble the distribution.
# The build context is the repository root (one level above this file).
# ─────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /work

# Copy Gradle wrapper and dependency metadata first so these layers
# are cached even when only source code changes.
COPY gradlew .
COPY gradle/ gradle/
COPY gradle.properties .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Pre-fetch dependencies (best-effort; a failure here is not fatal).
RUN ./gradlew dependencies --no-daemon -q 2>/dev/null || true

# Copy source and produce the application distribution.
# Tests are skipped here – they run as a separate CI step.
COPY src/ src/
RUN ./gradlew installDist --no-daemon -x test

# ─────────────────────────────────────────────────────────────────
# Stage 2 – Runtime
# Only the JRE and the assembled distribution are included.
# ─────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="mock-oauth2-server"
LABEL org.opencontainers.image.description="Mock OAuth2/OIDC server – fork with login_hint propagation support"
LABEL org.opencontainers.image.source="https://github.com/d-lopes/mock-oauth2-server"

WORKDIR /app

# Drop root privileges – run as an unprivileged user.
RUN addgroup -S app && adduser -S app -G app
USER app

# Copy the installDist output (bin/ + lib/).
COPY --from=builder /work/build/install/mock-oauth2-server/ .

# Required for Netty on Java 24+ (safe no-op on earlier versions).
# See https://netty.io/wiki/java-24-and-sun.misc.unsafe.html
ENV JAVA_TOOL_OPTIONS="--sun-misc-unsafe-memory-access=allow"

EXPOSE 8080

ENTRYPOINT ["bin/mock-oauth2-server"]
