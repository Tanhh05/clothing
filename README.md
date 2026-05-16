# Clothing Commerce Platform

Production-ready multi-surface commerce platform for fashion retail, including:

- `api-server`: Spring Boot backend API (auth, catalog, cart, checkout, orders, payments, notifications).
- `storefront`: Next.js customer-facing web storefront.
- `admin-portal`: Vue-based admin dashboard for operations.
- `clothing-ios`: iOS client app.

## Repository Structure

```text
clothing/
  api-server/      # Java Spring Boot backend
  storefront/      # Next.js storefront
  admin-portal/    # Vue admin dashboard
  clothing-ios/    # iOS app
  docs/            # Architecture, release, rollback, go-live docs
  scripts/         # Smoke tests and guard scripts
```

## Prerequisites

Install these tools first:

- Java 17+
- Maven 3.9+
- Node.js 20+ and npm
- Docker + Docker Compose (recommended for deployment/runtime)
- Git

## Quick Start (Local Development)

### 1) Clone the repository

```bash
git clone https://github.com/Tanhh05/clothing.git
cd clothing
```

### 2) Run Backend API

```bash
cd api-server
mvn spring-boot:run
```

Backend default URL: `http://localhost:8080`

### 3) Run Storefront

```bash
cd storefront
npm install
npm run dev
```

Storefront default URL: `http://localhost:3000`

### 4) Run Admin Portal

```bash
cd admin-portal
npm install
npm run dev
```

Admin default URL: `http://localhost:9528`

## Environment Variables

Check deployment/runtime env requirements in:

- `docs/env-matrix.md`
- `api-server/.env.example`

## Build and Test

### Backend

```bash
cd api-server
mvn -B -ntp test
```

### Storefront

```bash
cd storefront
npm run lint
npm run build
```

### Admin Portal

```bash
cd admin-portal
npm run test:unit -- --runInBand
npm run build:stage
```

## CI/CD and Operations

- CI workflow: `.github/workflows/ci.yml`
- Staging deploy: `.github/workflows/deploy-staging.yml`
- Production deploy: `.github/workflows/deploy-production.yml`
- API smoke test: `scripts/smoke-test-api.sh`
- Storefront smoke test: `scripts/smoke-test-storefront.sh`
- Admin smoke test: `scripts/smoke-test-admin.sh`

## Production Documentation

- Architecture: `docs/production-architecture.md`
- Release checklist: `docs/release-checklist.md`
- Rollback runbook: `docs/rollback-runbook.md`
- Go-live monitoring: `docs/go-live-watch.md`
- Execution status: `docs/execution-plan-status.md`

## License

Internal/private project unless otherwise specified by repository owner.
