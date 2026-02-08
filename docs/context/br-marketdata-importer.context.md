# Context — Broker: Marketdata Importer (Gateway)

Service: `br-marketdata-importer`
Location: `/apps/broker/br-marketdata-importer`
Role: Broker-side gateway that imports real-time market data and execution events from the Exchange distribution layer and publishes them into the Broker Kafka namespace; supports historical backfill requests initiated by Broker services.

Responsibilities:
(1) Real-time ingestion & forwarding
- Maintain a persistent socket/WebSocket connection to the Exchange distributor (`ex-marketdata-distributor`) to receive live price ticks and executed-order events.
- Normalize incoming events and publish them to Broker Kafka topics for downstream services.
- Detect missing sequence ranges or timestamp gaps and automatically request missing data from the Exchange.

(2) Historical backfill / resynchronization
- Accept REST requests from Broker services to request historical ticks for a symbol/time range (for recovery after offline periods).
- Forward backfill requests to the Exchange and stream retrieved historical data into Broker topics, preserving original timestamps and ordering.

(3) Scalability & resilience
- Persist offsets and minimal replay buffers for resumed consumption after restarts.
- Support batching and configurable write-through behavior for Kafka to balance latency and throughput.

Inputs:
- Socket/WebSocket: live ticks and execution events from Exchange distribution layer
- REST: backfill requests from broker-side services (symbol, start, end, broker-id)
- Admin: subscription and ACL configuration

Outputs:
- Broker Kafka topics:
  - `broker.market.ticks.{symbol}` — normalized live and backfill ticks
  - `broker.market.executions` — executed order events relevant to this Broker
  - `broker.market.backfill.{symbol}` — optional backfill stream or same topic with backfill metadata

Design & Operational Notes:
- Gap detection and backfill: detect missing messages using sequence numbers or monotonic timestamps; request missing ranges with exponential backoff and bounded retries.
- Ordering guarantees: ensure backfilled records are published before resuming live stream to maintain chronological order for downstream consumers.
- Backpressure & buffers: implement bounded buffers and backpressure signals; provide metrics for buffer saturation to prevent data loss.
- Schema management: use a schema registry (AVRO/Protobuf/JSON Schema) and versioning for tick and execution payloads.
- Observability: metrics for consumed messages/sec, published messages/sec, detected gaps, backfill latency, and Kafka publish failures.

Security & Privacy:
- Authenticate to Exchange endpoints and authorize backfill requests; propagate broker identity for scoped retrieval.
- Enforce TLS for socket connections and require authenticated API calls.

Demo Guidance:
- Provide a sample CLI or script that simulates a disconnected broker, then issues a backfill request and verifies ordered replay into Broker Kafka.
