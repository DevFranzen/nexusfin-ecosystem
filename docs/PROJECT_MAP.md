# Project-Map — NexusFin Ecosystem

## Overview
- **Project:** NexusFin Ecosystem
- **Purpose:** Demo and educational repository designed as a production-ready reference implementation; aims for production-grade architecture, quality, and reproducibility while serving demonstration use cases.
- **Essence:** A virtual trading ecosystem where an AI-driven Exchange publishes market data and a separate Broker ingests it to manage accounts, portfolios, and the order lifecycle in real time.

## System Landscapes
1. **Stock Exchange:**
   Simulates market data and market behavior. Prices are produced by an AI-driven market simulator.
2. **Broker:**
   Ingests real-time prices from Exchange, provides account/portfolio management, supports placing and cancelling orders, and maintains customer portfolios.

## Repository Structure
- Two strictly separated system landscapes (Exchange and Broker). Interaction only via official APIs; no shared runtime libraries.
- Single monorepo: all microservices live under `/apps/`.
  - Subfolders:
    - `/apps/exchange/` — Exchange-side services
    - `/apps/broker/` — Broker-side services
  - Service naming convention (mandatory):
    - `ex-...` prefix for Exchange-side services (located under `/apps/exchange/`).
    - `br-...` prefix for Broker-side services (located under `/apps/broker/`).

## Services Context:
The following context files are created for services present under `/apps/`:

- broker:
  - [br-marketdata-importer](docs/context/br-marketdata-importer.context.md)
  - [br-order-gateway](docs/context/br-order-gateway.context.md)
  - [br-marketdata-store](docs/context/br-marketdata-store.context.md)
  - [br-portfolio-manager](docs/context/br-portfolio-manager.context.md)
  - [br-router](docs/context/br-router.context.md)
  - [br-account-service](docs/context/br-account-service.context.md)
  - [br-gateway](docs/context/br-gateway.context.md)
  - [br-infra](docs/context/br-infra.context.md)
- exchange:
  - [ex-matching-engine](docs/context/ex-matching-engine.context.md)
  - [ex-order-manager](docs/context/ex-order-manager.context.md)
  - [ex-world-engine](docs/context/ex-world-engine.context.md)
  - [ex-marketdata-distributor](docs/context/ex-marketdata-distributor.context.md)
  - [ex-gateway](docs/context/ex-gateway.context.md)
  - [ex-infra](docs/context/ex-infra.context.md)
  - [ex-company-registry](docs/context/ex-company-registry.context.md)
  - [ex-price-generator](docs/context/ex-price-generator.context.md)


## Primary Tech Stack
- Java + Spring (Boot, Web, Kafka, AI, WebFlow)
- Apache Kafka
- OpenAPI - REST API contracts / Codegeneration
- Docker + Docker Compose for local demo

## Constraints
- Maintain strict separation between Exchange and Broker:
  - no shared services inside the monorepo
  - no shared runtime libraries
  - each system operates its own Kafka cluster
- Required communication channels: 
  - REST/OpenAPI for control and management
  - Kafka for real-time streaming and event driven flows

