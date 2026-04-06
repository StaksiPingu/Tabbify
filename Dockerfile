# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM gradle:8.8-jdk21 AS builder

WORKDIR /build

# 1. Copy only build files → own cache layer for dependencies
COPY backend/settings.gradle.kts backend/build.gradle.kts ./

# 2. Download dependencies separately → cached as long as build.gradle.kts unchanged
RUN gradle dependencies --no-daemon -q 2>/dev/null || true

# 3. Copy source + build
COPY backend/src/ src/
RUN gradle installDist --no-daemon

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S tabbify && adduser -S tabbify -G tabbify

COPY --from=builder /build/build/install/backend/ .
RUN mkdir -p /data/recordings && chown -R tabbify:tabbify /data/recordings /app

USER tabbify

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget -q -O- http://localhost:8080/health || exit 1

ENTRYPOINT ["bin/backend"]
