# Context — Broker: Marketdata Importer

Service: `br-marketdata-importer`
Location: `/apps/broker/br-marketdata-importer`
Role: Broker-side service that imports real-time market data and execution events from Exchange (via `br-router` proxy) and publishes them into the Broker Kafka namespace; supports historical backfill requests.

Responsibilities:
(1) Real-time ingestion & forwarding
- Request market data stream from Exchange via `br-router` proxy
- Normalize incoming events and publish them to Broker Kafka topics for downstream services
- Detect missing sequence ranges and automatically request backfill data

(2) Historical backfill / resynchronization
- Accept REST requests from Broker services to request historical ticks for a symbol/time range
- Forward backfill requests to Exchange via `br-router`
- Stream retrieved historical data into Broker topics, preserving original timestamps and ordering

(3) Resilience
- Persist offsets and minimal replay buffers for resumed consumption after restarts
- Handle connection failures and reconnection via `br-router`

Inputs:
- WebSocket/Stream: Live ticks and execution events from Exchange (proxied through `br-router`)
- REST: Backfill requests from broker-side services (symbol, start, end)
- Configuration: Symbols to subscribe to, gap detection settings

Outputs:
- Broker Kafka topics:
  - `broker.market.ticks.{symbol}` — normalized live and backfill ticks
  - `broker.market.executions` — executed order events relevant to this Broker
  - `broker.market.backfill.{symbol}` — optional backfill stream or same topic with backfill metadata

Design & Operational Notes:
- **Architecture**: ALL communication with Exchange goes through `br-router` proxy (never direct connection)
- Gap detection algorithm:
  1. Maintain in-memory map: symbol → last_sequence_number
  2. On each received tick:
     - If sequence_number != last_sequence_number + 1:
       - Log gap detected: [last_sequence_number, sequence_number]
       - Queue backfill request
     - Update last_sequence_number
  3. Backfill request via `br-router` to Exchange
  4. Resume live stream after backfill completes
- Gap detection configuration: To be defined in ADRs (detection window, retry policies)
- Ordering guarantees: Ensure backfilled records are published before resuming live stream to maintain chronological order
- Schema management: To be defined in ADRs (AVRO/Protobuf/JSON Schema)
- Observability: Metrics for consumed messages/sec, published messages/sec, detected gaps, backfill latency

Security & Privacy:
- Authentication handled by `br-router` (manages Exchange tokens)
- No direct Exchange connectivity from this service

Demo Guidance:
- Provide sample script that simulates gap detection and backfill
- Show ordered replay into Broker Kafka