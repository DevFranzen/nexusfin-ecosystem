# NexusFin Ecosystem

[![Architecture: Microservices](https://img.shields.io/badge/Architecture-Microservices-blue)](#system-landscapes)
[![Tech: Spring Boot](https://img.shields.io/badge/Tech-Spring%20Boot-brightgreen)](#primary-tech-stack)
[![Event-Driven: Kafka](https://img.shields.io/badge/Event--Driven-Apache%20Kafka-red)](#primary-tech-stack)

**NexusFin** is a high-fidelity, production-grade reference implementation of a distributed trading ecosystem. It simulates the complex interplay between a **Global Stock Exchange** and a **Retail Brokerage**, focusing on real-time event streaming, AI-driven market dynamics, and robust financial order lifecycles.

> [!NOTE]
> **Project Status: Architectural Planning & Initialization**
> This repository is currently in the active design and bootstrapping phase. Core architectural contexts and service boundaries are defined; implementation of the individual microservices is ongoing.

---

## Ecosystem Vision

The project relies on a strict "separation of concerns" at a systemic level. It treats the **Exchange** and the **Broker** as two entirely sovereign entities with zero shared code or shared runtime libraries.

### 1. The Stock Exchange (L1)
* **Market Simulation**: Prices are produced by an AI-driven market simulator.
* **AI-Driven Context**: An LLM-driven World Engine generates news items and aggregates them into market summaries.
* **Stochastic Modeling**: The Matching Engine uses Geometric Brownian Motion (GBM) to generate price ticks, with parameters (drift and volatility) derived from the AI-generated news context.
* **Authoritative Execution**: Acts as the single source of truth for the order lifecycle, emitting immutable execution events to ensure cross-system traceability and transactional integrity.

### 2. The Broker (L2)
* **Data Ingestion**: Ingests real-time prices from the Exchange to manage accounts and portfolios.
* **Resilience**: Detects missing sequence ranges or gaps and automatically requests historical backfills from the Exchange.
* **Portfolio Management**: Maintains positions and cash balances, calculating real-time valuations (P&L) using the latest market ticks.
* **Transaction Consistency**: Ensures ledger integrity by applying execution events idempotently and reconciling local portfolio states against the Exchange's authoritative event stream.

---

## System Architecture

The ecosystem is organized as a **monorepo** for discoverability, yet strictly partitioned to prevent architectural leakage.

```text
/apps/
├── exchange/                   # "The Market" - Price generation & matching
│   ├── ex-world-engine/        # LLM News & Scenario generation
│   ├── ex-matching-engine/     # Price simulation (GBM) & Orderbook
│   ├── ex-order-manager/       # Gateway for inbound broker orders
│   └── ex-marketdata-distributor/ # WebSocket/Stream distribution layer
└── broker/                     # "The Institution" - Portfolio & Client Mgmt
    ├── br-order-gateway/       # Client API & Validation
    ├── br-marketdata-importer/ # High-availability ingestion from Exchange
    ├── br-marketdata-store/    # Time-series persistence (TSDB)
    └── br-portfolio-manager/   # Position tracking & P&L calculation
```

## Primary Tech Stack

NexusFin leverages modern, enterprise-grade technologies to ensure scalability, resilience, and reproducibility:

* **Backend Framework:** Java 21 & Spring Boot 3.x (utilizing Spring Web, Spring AI, and Spring Kafka).
* **Event Streaming:** Apache Kafka for high-throughput market data distribution and asynchronous order lifecycles.
* **AI & LLM Integration:** Spring AI for news generation and contextual parameter estimation for price modeling.
* **Data Persistence:**
    * **Relational:** PostgreSQL for transactional consistency in account and order management.
    * **Time-Series:** TimescaleDB/InfluxDB for optimized storage and retrieval of tick data.
    * **Vector Search:** Qdrant/Milvus for semantic retrieval of synthetic news items.
* **API & Contracts:** OpenAPI (REST) for synchronous control flows and AsyncAPI for event-driven messaging.
* **Infrastructure:** Docker & Docker Compose for standardized local orchestration and demo environments.

---

## Key Engineering Patterns

* **Systemic Sovereignty:** The Exchange and Broker operate as entirely independent landscapes. Communication is strictly contract-based via APIs or Event Streams—no shared databases or internal libraries.
* **Stochastic Market Modeling:** Unlike static simulations, market prices are generated via Geometric Brownian Motion (GBM), where drift and volatility are dynamically adjusted based on AI-generated world events.
* **Self-Healing Data Streams:** The Broker-side ingestion layer implements gap detection using sequence numbers and automatically triggers historical backfills via the Exchange’s replay API.
* **Event-Sourced Valuation:** Portfolios are valued in real-time by subscribing to live tick streams, ensuring that P&L reflects the most recent market state with idempotent processing of execution events.

---

## Roadmap & Development Status

The project is currently in the **Architectural Definition & Bootstrapping Phase**. 

1.  **[x] Phase 1: Context Definition:** Completed service boundary specifications and interaction patterns.
2.  **[ ] Phase 2: Infrastructure Core:** Implementation of the Kafka Mesh and centralized Schema Registry.
3.  **[ ] Phase 3: Exchange Engine:** Development of the LLM-driven World Engine and GBM price generation.
4.  **[ ] Phase 4: Broker Lifecycle:** Implementation of the Order Gateway and Portfolio Management services.
5.  **[ ] Phase 5: Demo Layer:** Integration of a reference frontend/CLI for real-time visualization.

