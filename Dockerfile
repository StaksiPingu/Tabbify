# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM gradle:8.8-jdk21 AS builder

WORKDIR /build
COPY settings.gradle.kts build.gradle.kts gradle/ ./
COPY gradle/ gradle/
COPY backend/ backend/

RUN gradle :backend:installDist --no-daemon --stacktrace

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
