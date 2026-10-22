# syntax=docker/dockerfile:1
# ------------------------------------------------------------------------------
# BattleCode-UET Linux Worker Daemon (Oracle Free / Linux VM Compatible)
# Features: Multi-Runtime Bot Execution (Python, C++20, Java 21) + Bubblewrap Sandboxing
# ------------------------------------------------------------------------------

# --- Stage 1: Build Worker & Dependencies ---
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
COPY backend/pom.xml backend/pom.xml
COPY worker/pom.xml worker/pom.xml

COPY backend/src backend/src
COPY worker/src worker/src

RUN mvn -B clean package -DskipTests

# --- Stage 2: Sandboxed Linux Execution Environment ---
FROM ubuntu:24.04
ENV DEBIAN_FRONTEND=noninteractive

# Install student bot runtimes and sandboxing utilities
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-21-jdk-headless \
    python3 \
    g++ \
    bubblewrap \
    cgroup-tools \
    tar \
    gzip \
    curl \
    ca-certificates \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Create unprivileged runner user and sandbox directories
RUN useradd -m -s /bin/bash runner && \
    mkdir -p /tmp/battlecode-scratch /sys/fs/cgroup/battlecode && \
    chown -R runner:runner /tmp/battlecode-scratch /app

# Copy worker executable JAR from builder
COPY --from=builder /workspace/worker/target/battlecode-worker-*.jar /app/worker.jar

# Required worker configuration invariants
ENV WORKER_CONCURRENCY=1
ENV WORKER_SCRATCH_DIR=/tmp/battlecode-scratch
ENV WORKER_CGROUP_PARENT=/sys/fs/cgroup/battlecode
ENV JAVA_OPTS="-Xms64m -Xmx160m -XX:+UseSerialGC -XX:MaxMetaspaceSize=48m"

USER runner

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/worker.jar"]
