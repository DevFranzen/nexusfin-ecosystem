# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NexusFin is a high-fidelity, production-grade reference implementation of a distributed trading ecosystem simulating a **Global Stock Exchange** and a **Retail Brokerage** with real-time event streaming, AI-driven market dynamics, and robust financial order lifecycles.

**Current Status:** Architectural Planning & Initialization Phase. Core architectural contexts and service boundaries are defined; implementation of individual microservices is ongoing.

## Critical Architectural Rules

### Strict Domain Separation

The Exchange and Broker are **entirely sovereign entities** with zero shared code or runtime libraries:

- **Exchange services:** Located in `apps/exchange/`, prefixed with `ex-`
- **Broker services:** Located in `apps/broker/`, prefixed with `br-`
- **Communication only via:** REST/OpenAPI for control flows, Kafka for event streaming
- **No shared services** or runtime libraries between the two landscapes
- Each system operates its own Kafka cluster

### Context-First Development Protocol

**MANDATORY: Before working on any service in `apps/`, you MUST:**

1. Check if a context file exists in `docs/context/[service-name].context.md`
2. If the context file is missing or incomplete:
   - **STOP** - Do not generate implementation or logic
   - Inform the user that service-specific context is missing
   - Ask for the necessary context before proceeding

Context files define service responsibilities, inputs/outputs, Kafka topics, API contracts, and operational notes.

## Repository Structure

```
/apps/
├── exchange/              # Exchange-side services (ex-*)
│   ├── ex-world-engine/        # LLM news & scenario generation
│   ├── ex-matching-engine/     # GBM price simulation & orderbook
│   ├── ex-order-manager/       # Gateway for broker orders
│   ├── ex-marketdata-distributor/ # WebSocket distribution layer
│   ├── ex-gateway/             # Exchange API gateway
│   ├── ex-infra/               # Exchange infrastructure
│   ├── ex-company-registry/    # Company listing management
│   └── ex-price-generator/     # Price generation engine
└── broker/                # Broker-side services (br-*)
    ├── br-order-gateway/       # Client API & validation
    ├── br-marketdata-importer/ # High-availability ingestion from Exchange
    ├── br-marketdata-store/    # Time-series persistence
    ├── br-portfolio-manager/   # Position tracking & P&L calculation
    ├── br-account-service/     # Account management
    ├── br-gateway/             # Broker API gateway
    ├── br-infra/               # Broker infrastructure
    └── br-router/              # Broker routing layer

/docs/
├── context/               # Service-specific context files (*.context.md)
├── architecture/          # Architecture documentation
├── adr/                   # Architecture Decision Records
└── PROJECT_MAP.md         # High-level project map

nxf_partial_checkout_tool.sh  # Utility for sparse checkout of services
```

## Tech Stack

- **Backend:** Java 21 & Spring Boot 3.x (Spring Web, Spring AI, Spring Kafka)
- **Event Streaming:** Apache Kafka
- **AI/LLM:** Spring AI for news generation and market parameter estimation
- **Databases:**
  - PostgreSQL (transactional data)
  - TimescaleDB/InfluxDB (time-series tick data)
  - Qdrant/Milvus (vector search for news)
- **API Contracts:** OpenAPI (REST), AsyncAPI (event-driven)
- **Infrastructure:** Docker & Docker Compose

## Key Engineering Patterns

### 1. AI-Driven Stochastic Market Modeling

- **World Engine (`ex-world-engine`):** LLM generates news items by category, aggregates into market summaries
- **Matching Engine (`ex-matching-engine`):** Uses Geometric Brownian Motion (GBM) to generate price ticks
  - GBM parameters (drift, volatility) derived from AI-generated news context
  - Scheduled price generation: one hour ahead of real time
  - Deterministic RNG seeds for reproducible demos

### 2. Self-Healing Data Streams

- **Broker Marketdata Importer (`br-marketdata-importer`):**
  - Detects missing sequence ranges or gaps using sequence numbers
  - Automatically requests historical backfills from Exchange
  - Maintains chronological order when replaying historical data

### 3. Event-Sourced Valuation

- Portfolios valued in real-time by subscribing to live tick streams
- Idempotent processing of execution events
- P&L reflects most recent market state

### 4. Order Lifecycle Flow

**Exchange perspective:**
1. Orders received via `ex-order-manager` REST API
2. `ex-matching-engine` maintains orderbook, evaluates against ticks
3. Execution events published to `exchange.trades.executed` Kafka topic

**Broker perspective:**
1. Client orders via `br-order-gateway` REST/WebSocket API
2. Validated orders published to `broker.orders.submitted` Kafka topic
3. Broker processes orders and forwards to Exchange
4. Execution events flow: Exchange → `br-marketdata-importer` → Broker Kafka → Client

## Language & Communication

- **All code, comments, docs, commit messages MUST be in English**
- Variable naming, function names, classes: English only
- Response language: Always English

## Code Style

- Prefer interfaces over types
- Strict null checks mandatory
- Use standard Spring Boot conventions

## Development Workflow

Since services are in the bootstrapping phase, typical build/test commands are not yet established. When implementing services:

1. Consult the context file in `docs/context/[service-name].context.md`
2. Follow Spring Boot 3.x conventions for project structure
3. Define OpenAPI contracts for REST endpoints
4. Define AsyncAPI contracts for Kafka topics
5. Use the naming conventions in the context files for Kafka topics:
   - Exchange: `exchange.market.*`, `exchange.trades.*`, `exchange.orders.*`
   - Broker: `broker.market.*`, `broker.orders.*`

## Partial Checkout Tool

For working on individual services in isolation, use `nxf_partial_checkout_tool.sh`:

```bash
./nxf_partial_checkout_tool.sh
# Select a service number or 'a' for all services
# Creates sparse checkouts in ../nexusfin-ecosystem-partial-co/
```

## Reference Documentation

- **PROJECT_MAP.md:** High-level system overview and service registry
- **README.md:** Ecosystem vision, architecture, and roadmap
- **docs/context/:** Detailed service specifications with responsibilities, I/O, Kafka topics, and API contracts
