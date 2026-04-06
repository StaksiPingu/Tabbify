# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM gradle:8.8-jdk21 AS builder

WORKDIR /build

# 1. Nur Build-Config kopieren → eigener Cache-Layer für Dependencies
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY backend/build.gradle.kts backend/

# 2. Dependencies separat herunterladen → wird gecacht solange build.gradle.kts gleich bleibt
RUN gradle :backend:dependencies --no-daemon -q 2>/dev/null || true

# 3. Quellcode kopieren + bauen
COPY backend/src/ backend/src/
RUN gradle :backend:installDist --no-daemon

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S tabbify && adduser -S tabbify -G tabbify

COPY --from=builder /build/backend/build/install/backend/ .
RUN mkdir -p /data/recordings && chown -R tabbify:tabbify /data/recordings /app

USER tabbify

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget -q -O- http://localhost:8080/health || exit 1

ENTRYPOINT ["bin/backend"]
