# Context — Exchange: Marketdata Distributor (Gateway)

Service: `ex-marketdata-distributor`
Location: `/apps/exchange/ex-marketdata-distributor`
Role: Gateway service that exposes real-time market data and execution events to Broker clients via socket connections and REST, acts as a filtered per-broker distribution layer.

Responsibilities:
(1) Real-time distribution
- Subscribe to real-time price tick topics (published by `ex-matching-engine`) and stream relevant market prices to connected Broker clients over WebSocket/SSE or per-broker Kafka channels.
- The service acts as a gateway: it does not generate prices, it consumes the `exchange.market.{symbol}.ticks` topic and forwards normalized ticks to authorized broker connections.

(2) Historical retrieval / backfill
- Provide REST endpoints to retrieve historical ticks from the persisted ticks store to allow brokers to resynchronize after offline periods or network issues.
- Support ranged queries, pagination, and replay semantics (replay in original time-order) and allow clients to request a time-aligned backfill.

(3) Per-broker execution event delivery
- Subscribe to execution/event topics (e.g., `exchange.trades.executed`) and deliver executed orders only to the broker that placed the order (per-broker delivery channels).
- Ensure execution events include sufficient metadata (order id, execution price, quantity, timestamp) while honoring privacy constraints.

(4) Scaling & future outlook
- Design for multiple instances behind a load-balancer and consider sharding by broker-id or symbol-range for stateful subscriptions and resumption.

Inputs:
- Kafka topics: `exchange.market.{symbol}.ticks` (real-time ticks), `exchange.trades.executed` (executions)
- Database: persisted `ticks` table (for backfill and replay)
- Admin API: subscription configuration, ACLs, format preferences

Outputs / External Interfaces:
- WebSocket / SSE endpoints: per-broker subscriptions to live ticks and execution events
- REST API: historical tick retrieval, subscription management, health and metrics
- Optional per-broker Kafka topics or push endpoints for brokers that prefer Kafka pull/push

Design & Operational Notes:
- Authorization: authenticate and authorize broker clients; use per-broker credentials and scopes to limit subscribed symbols and allowed operations.
- Backfill: implement efficient range queries and chunked replay; allow clients to request a resume token or last-processed timestamp.
- Delivery semantics: support at-least-once delivery for real-time streaming with optional client-side deduplication, and exactly-once semantics for execution events where possible.
- Format & schema: normalize tick payloads and use a schema registry for versioned schemas (JSON/AVRO/Protobuf as required).
- Observability: expose metrics for active connections, messages/sec, lag from Kafka, and backfill durations; log delivered execution events per-broker.
- Operational concerns: rate-limiting, per-connection quotas, TLS for sockets, and health checks for load balancer.

Security & Privacy:
- Enforce encryption in transit (TLS) and require authenticated connections.
- Ensure execution events are only visible to the originating broker.

Demo Guidance:
- Provide a lightweight reference client (WebSocket) that authenticates, subscribes to a symbol, and demonstrates live tick streaming and backfill.

