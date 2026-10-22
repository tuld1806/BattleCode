# BattleCode-UET Cloud Deployment Guide

This guide describes how to deploy the full BattleCode-UET platform at **zero service spend** using free tiers from Render, Cloudflare Pages, Supabase, and an Oracle/Linux VM.

---

## Architecture Topology

```
┌───────────────────────────────┐
│ Cloudflare Pages (Frontend)   │ <── Students & Spectators
│ (Static Export from out/)     │     (Zero Egress, Global CDN)
└──────────────┬────────────────┘
               │ HTTPS (REST API)
               ▼
┌───────────────────────────────┐
│ Render Free Web Service       │ <── Spring Boot 3.3.4 (Java 21)
│ (battlecode-api, 512MB RAM)   │     Port 8080, Serial GC
└───────┬──────────────┬────────┘
        │              │
        ▼              ▼
┌──────────────┐ ┌───────────────────────────┐
│ Supabase     │ │ Cloudflare R2             │
│ PostgreSQL   │ │ (Bot Binaries & Replays)  │
│ (Port 6543)  │ └─────────────┬─────────────┘
└──────────────┘               │ Download/Upload
                               ▼
                 ┌───────────────────────────┐
                 │ Linux Worker VM           │
                 │ (bwrap + cgroups sandbox) │
                 └───────────────────────────┘
```

---

## 1. Supabase Setup (Database & Authentication)

1. Create a free Supabase project at [supabase.com](https://supabase.com).
2. Note the database connection string from **Database Settings &rarr; Connection String &rarr; Connection Pooling**.
   - Use the **Transaction Pooler (Port 6543)**:
     ```
     jdbc:postgresql://[host]:6543/postgres?pgbouncer=true&prepareThreshold=0
     ```
   - *Invariant:* Hikari pool size must not exceed `2` per service.
3. Flyway migrations in `backend/src/main/resources/db/migration/V1__init.sql` will automatically run upon Spring Boot startup.

---

## 2. Cloudflare R2 Setup (Artifact Storage)

1. Navigate to **Cloudflare Dashboard &rarr; R2**.
2. Create bucket named `battlecode-artifacts`.
3. Create API Token with Object Read & Write permissions.
4. Record:
   - `R2_ENDPOINT` (e.g. `https://<account_id>.r2.cloudflarestorage.com`)
   - `R2_ACCESS_KEY_ID`
   - `R2_SECRET_ACCESS_KEY`
   - `R2_BUCKET`: `battlecode-artifacts`
   - `R2_PUBLIC_BASE_URL`: Public custom subdomain or r2.dev URL.

---

## 3. Render Free Backend Deployment

1. Connect your repository to Render.
2. Select **New &rarr; Blueprint** and select `render.yaml`.
3. Provide the environment variables configured in Supabase and R2.
4. Health check endpoint: `GET /api/public/tournaments`.

---

## 4. Cloudflare Pages Frontend Deployment

1. Navigate to **Cloudflare Dashboard &rarr; Pages &rarr; Create Project &rarr; Connect to Git**.
2. Build Settings:
   - **Framework preset:** `Next.js (Static HTML Export)`
   - **Build command:** `cd frontend && npm install && npm run build`
   - **Build output directory:** `frontend/out`
3. Environment variables:
   - `NEXT_PUBLIC_API_BASE_URL`: URL of the Render backend.
   - `NEXT_PUBLIC_SUPABASE_URL`: Supabase project URL.
   - `NEXT_PUBLIC_SUPABASE_ANON_KEY`: Supabase anon key.

---

## 5. Linux Match Worker Daemon Setup

1. Provision an Ubuntu 24.04 VM (e.g. Oracle Always Free 4 OCPU / 24GB Arm).
2. Install dependencies:
   ```bash
   sudo apt-get update && sudo apt-get install -y \
     openjdk-21-jdk-headless bubblewrap cgroup-tools python3 g++ tar gzip
   ```
3. Set environment variables in `/etc/environment` or systemd service:
   ```bash
   BATTLECODE_API_BASE="https://your-api.onrender.com"
   WORKER_API_KEY="your-secret-worker-key"
   WORKER_ID="worker-linux-01"
   WORKER_CONCURRENCY="1"
   WORKER_SCRATCH_DIR="/tmp/battlecode-scratch"
   WORKER_CGROUP_PARENT="/sys/fs/cgroup/battlecode"
   R2_ENDPOINT="https://<id>.r2.cloudflarestorage.com"
   R2_ACCESS_KEY_ID="..."
   R2_SECRET_ACCESS_KEY="..."
   R2_BUCKET="battlecode-artifacts"
   R2_PUBLIC_BASE_URL="https://r2.yourdomain.com"
   ```
4. Run the worker daemon:
   ```bash
   java -Xms64m -Xmx160m -XX:+UseSerialGC -XX:MaxMetaspaceSize=48m -jar worker.jar
   ```
