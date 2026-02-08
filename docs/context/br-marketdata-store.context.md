# Context — Broker: Marketdata Store

Service: `br-marketdata-store`
Location: `/apps/broker/br-marketdata-store`
Role: Time-series persistence and query service for Broker-side market data and backfill archives. Serves as the authoritative store for historical ticks used for replay, analytics, and resynchronization.

Responsibilities:
- Persist normalized ticks and backfill batches received from `br-marketdata-importer` into a time-series optimized store.
- Provide REST and query APIs for range queries, aggregation, and pagination for Broker services requesting historical data.
- Store execution events relevant to the Broker for audit and reconciliation.
- Retention and compaction policies: implement retention windows, optional downsampling, and archiving for older data.

Inputs:
- Broker Kafka topics: `broker.market.ticks.{symbol}`, `broker.market.backfill.{symbol}`, `broker.market.executions`
- Backfill ingestion from `br-marketdata-importer` (streamed or batch)

Outputs / External Interfaces:
- REST API: `/api/v1/ticks?symbol={symbol}&start={}&end={}&limit={}` — returns ordered tick data or aggregates.
- Admin API for retention/compaction control and status.
- Optional export endpoints for analytics or external consumers.

Design & Operational Notes:
- Storage choices: recommend Postgres+TimescaleDB or a dedicated TSDB (InfluxDB) depending on query patterns; use partitioning by symbol and time range.
- Indexing: ensure efficient time-range indexes and optional secondary indexes for query by symbol, resolution, or data tags.
- Backfill ingestion: support idempotent writes and conflict resolution when overlapping backfill ranges occur.
- Compaction/downsampling: support configurable downsampling for older data to reduce storage costs while preserving fidelity for recent windows.
- Backup & archive: provide export tooling for long-term archival.

Security & Privacy:
- Authenticate and authorize service calls; only internal Broker services should access this store.
- Ensure RBAC for admin operations (retention, compaction, exports).

Observability:
- Expose metrics: write throughput, read qps, query latencies, storage utilization, and backfill ingest success/failure rates.

Demo Guidance:
- Seed a small dataset and provide example queries and a simple UI or CLI to demonstrate replay and aggregation functionality.
