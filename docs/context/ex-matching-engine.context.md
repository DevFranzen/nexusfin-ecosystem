# Context — Exchange: Matching Engine

Service: `ex-matching-engine`
Location: `/apps/exchange/ex-matching-engine`
Role: Exchange-side service responsible for company listing management, scheduled price generation (GBM), real-time tick streaming, and orderbook management/execution.

Responsibilities:
1. Company listing management
   - Maintain the registry of listed (fictional) companies and their metadata.
   - Provide an API to "list" a company; when listing, derive a firm's background story and market-relevant start parameters using an LLM prompt based on supplied seed parameters.

2. Scheduled price generation (hour-ahead)
   - A scheduled sub-process iterates over all listed companies and generates price ticks for the next full hour (i.e., one hour ahead of real time).
   - Prices are modeled using Geometric Brownian Motion (GBM). GBM input parameters (drift, volatility, initial price) are derived from the current news context via an LLM.
   - To obtain news context, the service calls `ex-world-engine` API with a request containing firm identifiers and a time window; `ex-world-engine` returns relevant news via vector search.
   - Using the LLM-derived parameters, the GBM path for the upcoming hour is computed and persisted in the database (tick records with timestamps for that hour).

2a. GBM modeling details
   - Implement GBM simulation with configurable timestep resolution and seedable RNG for deterministic demo runs.
   - Allow configuration overrides for drift/volatility for scenario-driven tests.

3. Real-time tick streaming
   - A runtime process reads persisted ticks for the current time window and streams them in real time to a Kafka topic (publishes the current price updates as they occur).
   - Topic(s): `exchange.market.{symbol}.ticks`

4. Orderbook and order execution
   - Maintain an in-memory or persisted orderbook per symbol.
   - Expose REST/OpenAPI endpoints to place and cancel orders. Orders include fields (side, quantity, price type, limit/market, timestamp, client id).
   - Evaluate incoming orders against market ticks and orderbook rules; when execution conditions are met, mark orders executed and publish execution events.
   - Execution events published to Kafka topic: `exchange.trades.executed` (include order id, executed price, quantity, timestamp).

Inputs:
- API: Company listing requests (seed params)
- API: Requests to `ex-world-engine` for news/context (firm id, timeframe)
- Incoming orders via REST/OpenAPI
- Configuration: scheduling frequency, randomness/seed for repeatability, GBM model defaults

Outputs:
- Persisted DB tables: `companies`, `ticks`, `orderbook`, `executions` (schema to be defined)
- Kafka topics:
  - `exchange.market.{symbol}.ticks` (price ticks streamed in real time)
  - `exchange.trades.executed` (executed orders)
  - `exchange.orders.events` (order lifecycle events)

Design & Operational Notes:
- GBM modeling: implement a deterministic random seed option in config so hourly-generation is repeatable for demo scenarios.
- LLM integration: use `ex-world-engine` news retrieval plus firm metadata to build an LLM prompt that outputs drift/volatility estimates and scenario tags.
- Persistence: generated hourly ticks must be persisted before the hour starts to allow replay and deterministic streaming.
- Streaming: the streaming component should read ticks from the DB and publish them at wall-clock time aligned to tick timestamps.
- Order execution: execution logic can be conservative (match against last published tick and simple orderbook rules) for demo clarity.
- Observability: log LLM prompts/responses, GBM parameter values, DB writes for ticks, and published Kafka messages.

Security & Privacy:
- Do not persist or expose personally identifiable information in company background stories; synthesize any necessary personal details.

Demo Guidance:
- Provide an admin API to trigger hourly-generation on demand for demos.
- Document the request format used to ask `ex-world-engine` for news so consumers can reproduce scenario runs.
