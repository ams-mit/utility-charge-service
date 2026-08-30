# utility-charge-service

AMS Group 3 — Utility Charge Recording and Calculation  
Port: **8082** | Database: **utility_db**

## Purpose
Records water, electricity, gas, and parking usage per unit per billing period.  
Feeds utility charge data to `billing-payment-service` during invoice generation via internal API.

## Tech Stack
Spring Boot 3.3 · Java 21 · MySQL · Spring Security · JWT · Springdoc OpenAPI

## Quick Start

### Prerequisites
- Java 21, Maven 3.9+, MySQL 8+

### Setup
```bash
cp .env.example .env
# Fill in .env values

mysql -u root -p -e "CREATE DATABASE utility_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p utility_db < src/main/resources/schema.sql
```

### Run
```bash
./mvnw spring-boot:run
```

### API Docs
Swagger UI: http://localhost:8082/swagger-ui.html  
Health: http://localhost:8082/actuator/health

## Branch Strategy
- `main` — protected, production-stable
- `dev` — integration branch
- `feature/AMS-G3-{issue}-{desc}` — working branches

## API Contract
See `project-docs/api-contracts/` for the full contract reference.  
Internal endpoint `GET /api/v1/internal/utility-charges/units/{unitId}?year=&month=` is Docker-network only.