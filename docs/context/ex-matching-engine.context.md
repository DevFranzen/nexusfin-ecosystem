# Context — Exchange: Matching Engine

Service: `ex-matching-engine`
Location: `/apps/exchange/ex-matching-engine`
Role: Exchange-side service responsible for mock price generation (GBM), real-time tick streaming, and orderbook management/execution.

Responsibilities:
1. Mock price generation (hour-ahead) **[DEMO LOGIC - LOOSELY COUPLED]**
   - **IMPORTANT**: This is synthetic price generation for demo purposes only. In a production system, price ticks would be generated from actual order matching and executions.
   - This mock logic is intentionally loosely coupled and designed to be easily replaced with real matching engine logic.
   - A scheduled sub-process generates mock price ticks for the next full hour using Geometric Brownian Motion (GBM).
   - Subscribes to `exchange.companies.events` from `ex-company-registry` to know which companies to generate prices for.
   - GBM input parameters (drift, volatility, initial price) are derived from the current news context via an LLM.
   - To obtain news context, the service calls `ex-world-engine` API with a request containing firm identifiers and a time window; `ex-world-engine` returns relevant news via vector search.
   - Using the LLM-derived parameters, the GBM path for the upcoming hour is computed and persisted in the database (tick records with timestamps for that hour).

1a. GBM modeling details (Mock Implementation)
   - Implement GBM simulation with configurable timestep resolution and seedable RNG for deterministic demo runs.
   - Allow configuration overrides for drift/volatility for scenario-driven tests.
   - **Loose Coupling**: The GBM price generator is a pluggable component that can be swapped with real order-driven price generation.

2. Real-time tick streaming
   - A runtime process reads persisted ticks for the current time window and streams them in real time to a Kafka topic (publishes the current price updates as they occur).
   - Topic(s): `exchange.market.{symbol}.ticks`

3. Orderbook and order execution
   - Maintain an in-memory or persisted orderbook per symbol.
   - Expose REST/OpenAPI endpoints to place and cancel orders. Orders include fields (side, quantity, price type, limit/market, timestamp, client id).
   - Evaluate incoming orders against market ticks and orderbook rules; when execution conditions are met, mark orders executed and publish execution events.
   - Execution events published to Kafka topic: `exchange.trades.executed` (include order id, executed price, quantity, timestamp).

Inputs:
- Kafka topic: `exchange.companies.events` (from `ex-company-registry` for newly listed companies)
- API: Requests to `ex-world-engine` for news/context (firm id, timeframe)
- Incoming orders via REST/OpenAPI from `ex-order-manager`
- Configuration: scheduling frequency, randomness/seed for repeatability, GBM model defaults

Outputs:
- Persisted DB tables: `ticks`, `orderbook`, `executions` (schema to be defined)
- Kafka topics:
  - `exchange.market.{symbol}.ticks` (price ticks streamed in real time)
  - `exchange.trades.executed` (executed orders)
  - `exchange.orders.events` (order lifecycle events)

Design & Operational Notes:
- **Mock Price Generation - Loose Coupling**:
  - The GBM-based price generator is a pluggable component for demo purposes
  - In production, replace with real matching engine that generates ticks from actual order executions
  - Interface separation allows easy swap between mock (GBM) and real (order-driven) price generation
- GBM modeling: implement a deterministic random seed option in config so hourly-generation is repeatable for demo scenarios.
- LLM integration: use `ex-world-engine` news retrieval plus firm metadata to build an LLM prompt that outputs drift/volatility estimates and scenario tags.
- Persistence: generated hourly ticks must be persisted before the hour starts to allow replay and deterministic streaming.
- Streaming: the streaming component should read ticks from the DB and publish them at wall-clock time aligned to tick timestamps.
- Order execution: execution logic can be conservative (match against last published tick and simple orderbook rules) for demo clarity.
- Company registry integration: Subscribe to `exchange.companies.events` to initialize price generation for newly listed companies.
- Observability: log LLM prompts/responses, GBM parameter values, DB writes for ticks, and published Kafka messages.

Security & Privacy:
- No PII handling in this service (company background stories are managed by `ex-company-registry`).

Demo Guidance:
- Provide an admin API to trigger hourly-generation on demand for demos.
- Document the request format used to ask `ex-world-engine` for news so consumers can reproduce scenario runs.
