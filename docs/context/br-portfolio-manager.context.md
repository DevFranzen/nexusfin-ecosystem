# Context — Broker: Portfolio Manager

Service: `br-portfolio-manager`
Location: `/apps/broker/br-portfolio-manager`
Role: Broker-side service responsible for managing client portfolios, positions, valuations, and providing portfolio-related APIs to other Broker services and clients.

Responsibilities:
- Maintain account portfolios, positions, and cash balances.
- Provide valuation and P&L calculations using latest market prices (subscribe to `broker.market.ticks.{symbol}`).
- Apply fills from executed orders to update positions and balances; publish portfolio-events for downstream consumption.
- Expose REST/OpenAPI endpoints for portfolio queries, position listings, and valuation snapshots.

Inputs:
- Kafka topics: `broker.market.ticks.{symbol}`, `broker.market.executions`
- REST: portfolio management requests (view, adjustments, snapshots)
- Authenticated client/broker identity for scoped access

Outputs:
- Kafka topics: `broker.portfolios.events` (position updates, valuation changes, reconciliation events)
- REST responses for portfolio queries and snapshots

Design & Operational Notes:
- Data model: use relational store (Postgres) for transactional consistency (accounts, positions, ledger entries) and consider caching for read-heavy valuation queries.
- Consistency: apply executions idempotently; use event-sourcing or append-only ledger for auditability.
- Valuations: subscribe to Broker market tick topics and maintain an LRU cache of latest prices for quick valuation; support on-demand revaluation using `br-marketdata-store` for historical pricing.
- Reconciliation: provide endpoints and batch jobs to reconcile positions against execution history and ledger.
- Security: enforce per-broker and per-client access controls; redact sensitive info in logs.
- Observability: expose metrics for portfolio counts, valuation latencies, reconciliation errors, and event publish rates.

Demo Guidance:
- Provide sample API calls for retrieving a portfolio snapshot and an example reconciliation run.
