# syntax=docker/dockerfile:1
# ------------------------------------------------------------------------------
# BattleCode-UET Backend Spring Boot API (Render Free 512MB RAM Optimized)
# Target Java Version: Java 21 (Eclipse Temurin)
# ------------------------------------------------------------------------------

# --- Stage 1: Build Application ---
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy root pom and backend descriptor for dependency caching
COPY pom.xml .
COPY backend/pom.xml backend/pom.xml
COPY worker/pom.xml worker/pom.xml
RUN mvn -B -f backend/pom.xml dependency:go-offline || true

# Copy source and package executable JAR
COPY backend/src backend/src
RUN mvn -B -f backend/pom.xml clean package -DskipTests

# --- Stage 2: Minimal Production JRE Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: Run as unprivileged service user
RUN addgroup -S battlecode && adduser -S battlecode -G battlecode
USER battlecode:battlecode

# Copy fat jar from builder
COPY --from=builder --chown=battlecode:battlecode /workspace/backend/target/battlecode-backend-*-exec.jar /app/app.jar

# Render Free 512MB RAM Guardrails (Max 160MB Heap + 128MB Metaspace < 512MB RAM)
ENV JAVA_OPTS="-Xms64m -Xmx160m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:${PORT}/api/public/tournaments || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
