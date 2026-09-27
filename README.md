# utility-charge-service

AMS Group 3 — Utility Charge Recording and Calculation
Port: **8082** | Database: **utility_db**

## Purpose
Records water, electricity, gas, and parking usage per unit per billing period.
Feeds utility charge data to `billing-payment-service` during invoice generation via internal API.

## Tech Stack
Spring Boot 3.3 · Java 21 · MySQL · Spring Security · JWT (RS256) · Springdoc OpenAPI

## Features
- **Utility Rates** (`UTIL-001` – `UTIL-005`) — create, list, view, update, and activate/deactivate per-unit-type utility rates.
- **Utility Charges** (`UTIL-006` – `UTIL-013`) — record, list, view, update, and delete utility usage per unit per billing period, with per-unit and per-period lookups and a summary endpoint.
- **Internal API** — Docker-network-only endpoint that feeds utility charge totals to `billing-payment-service` for invoice generation.
- **Role isolation** — `FINANCE_OFFICER` and `APARTMENT_MANAGER` can view all units; `RESIDENT`, `TENANT`, and `OWNER` are restricted to their own `unitId`.
- **Dev token issuer** — local-only endpoint for generating test JWTs during development.

## Quick Start

### Prerequisites
- Java 21, Maven 3.9+, MySQL 8+

### Setup
```bash
cp .env.example .env
# Fill in .env values (DB credentials, Gateway/Service JWT keys, API Gateway URL, CORS origin)

mysql -u root -p -e "CREATE DATABASE utility_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p utility_db < src/main/resources/schema.sql
```

### Run
```bash
./mvnw spring-boot:run
```

### Run with Docker
```bash
docker build -t utility-charge-service .
docker run --env-file .env -p 8082:8082 utility-charge-service
```

### API Docs
Swagger UI: http://localhost:8082/swagger-ui.html
Health: http://localhost:8082/actuator/health


## API Overview

**Utility Rates** — `/api/v1/utility-rates`
Full CRUD, plus `PATCH /{utilityRateId}/status` to activate/deactivate a rate.

**Utility Charges** — `/api/v1/utility-charges`
Full CRUD, plus:
- `GET /units/{unitId}` — all charges for a unit
- `GET /units/{unitId}/period/{year}/{month}` — charges for a specific billing period
- `GET /summary` — aggregated summary

**Internal API** — `GET /api/v1/internal/utility-charges/units/{unitId}?year=&month=`
Docker-network only. Consumed by `billing-payment-service` during invoice generation.

**Dev Tools** — `GET /dev/token`
Local-only endpoint for issuing test JWTs during development.

Full endpoint-level reference: [`MD_Doc/AMS_G3_API_Reference_v2_1.md`](MD_Doc/AMS_G3_API_Reference_v2_1.md) and [`MD_Doc/project-a-canonical-complete-api-endpoint-registry.md`](MD_Doc/project-a-canonical-complete-api-endpoint-registry%20.md).

## Architecture Notes
- All inter-service calls go through the API Gateway (`API_GATEWAY_URL`) — services never call each other directly.
- Incoming Gateway JWTs are verified with `GATEWAY_JWT_PUBLIC_KEY`; outgoing service-to-service JWTs are signed with `SERVICE_JWT_PRIVATE_KEY` (5-minute expiry).
- `client/RealBillingServiceClient` and `client/MockBillingServiceClient` implement `BillingServiceClient`, so billing integration can be toggled for local development/testing.

## CI/CD & Deployment
- **CI**: `.github/workflows/ci.yml` — build and test on push/PR.
- **Deploy**: `.github/workflows/deploy.yml` — builds and pushes the Docker image, deploys via Terraform.
- **Infra**: `terraform/` provisions an Azure Container App, Azure Container Registry, and an Azure MySQL Flexible Server (see `terraform/outputs.tf`).

## Branch Strategy
- `main` — protected, production-stable
- `dev` — integration branch
- `feature/AMS-G3-{issue}-{desc}` — working branches

## API Contract
See `project-docs/api-contracts/` for the full contract reference.
Internal endpoint `GET /api/v1/internal/utility-charges/units/{unitId}?year=&month=` is Docker-network only.