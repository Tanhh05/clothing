# Production Architecture (Clothing)

## 1) Environments
- `dev` (local): docker compose + local env.
- `staging`: production-like, auto deploy from `main` after CI pass.
- `prod`: manual approval deploy only.

## 2) Runtime Topology
- `storefront` (Next.js) behind CDN + HTTPS.
- `admin-portal` (Vue admin) behind HTTPS + IP restriction (recommended).
- `api-server` (Spring Boot) behind reverse proxy (Nginx/Caddy) + HTTPS.
- `postgres` managed or VM-hosted with daily backup.
- `redis` for cache/session acceleration.
- `rabbitmq` for async jobs.
- `elasticsearch` for search.
- `object storage` (Cloudflare R2) for product/media assets.

## 3) Domain Plan
- `haru.vn` -> storefront
- `admin.haru.vn` -> admin portal
- `api.haru.vn` -> api server
- `cdn.haru.vn` -> static/media assets

## 4) Network & Security Baseline
- TLS everywhere (Let's Encrypt or managed cert).
- HSTS + secure headers at reverse proxy:
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Content-Security-Policy` (phased rollout)
- CORS allowlist by env:
  - prod: `https://haru.vn,https://admin.haru.vn`
  - staging: staging domains only.
- Admin routes protected by role + optional VPN/IP allowlist.

## 5) Secrets & Config
- No real secret in repo.
- Secrets source of truth:
  - GitHub Environments (`staging`, `production`)
  - runtime env files on server owned by ops.
- Rotate these every 90 days:
  - `JWT_SECRET`, `DB_PASSWORD`, `R2_SECRET_KEY`, payment keys.

## 6) Data & Backup
- Flyway migration mandatory on startup.
- Postgres:
  - daily full backup + WAL/point-in-time if possible.
  - retention: 14 days staging, 30 days prod.
- R2 lifecycle and versioning for uploads.

## 7) Observability
- Centralized logs: app + proxy + db alerts.
- Metrics:
  - API latency p95/p99
  - error rate 4xx/5xx
  - queue depth (RabbitMQ)
  - DB CPU/disk
- Alerts:
  - API health down > 2m
  - 5xx spike > 2%
  - payment callback failures spike.

## 8) Deploy Strategy
- CI required checks:
  - secret guard
  - backend tests
  - frontend lint/build
  - smoke test after deploy.
- Production deploy:
  - manual approval gate.
  - blue/green or rolling with quick rollback tag.

