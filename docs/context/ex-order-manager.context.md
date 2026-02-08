# Context — Exchange: Order Manager (Gateway)

Service: `ex-order-manager`
Location: `/apps/exchange/ex-order-manager`
Role: Central gateway into the Exchange for order lifecycle interactions. Exposes REST/OpenAPI for Brokers to place, cancel, and query orders and orchestrates forwarding to `ex-matching-engine`.

Responsibilities:
(1) Place orders (asynchronous execution)
- Accept buy/sell orders via REST API (validated request payload).
- Forward orders to `ex-matching-engine` for placement (preferably via Kafka topic `exchange.orders.submitted` or a dedicated async endpoint).
- Respond immediately with an order acknowledgement containing a generated `orderId`, submission timestamp, and status `PLACED` or `REJECTED`.
- Execution is asynchronous; when an order is executed, the execution event is delivered to the originating broker by `ex-marketdata-distributor`.

(2) Cancel orders (synchronous)
- Accept cancel requests via REST API and synchronously call `ex-matching-engine` to attempt cancellation.
- Return synchronous success/failure and reason. If cancel succeeds, publish an order lifecycle event to `exchange.orders.events`.

(3) Query order status
- Expose REST API to query current order status by `orderId` (fields: orderId, status, filledQty, remainingQty, lastUpdated, executions[]).
- Provide a way for brokers to poll status to reconcile open orders.

Inputs:
- REST: `POST /api/v1/orders` (place order), `POST /api/v1/orders/{orderId}/cancel` (cancel), `GET /api/v1/orders/{orderId}` (status)
- Kafka: optional inbound control topics for admin commands
- Events: execution confirmations from `ex-matching-engine` (if matching publishes executions), and/or from `ex-marketdata-distributor` for per-broker delivery acknowledgements

Outputs:
- Kafka topics:
  - `exchange.orders.submitted` (order submission to matching engine)
  - `exchange.orders.events` (order lifecycle events: PLACED, CANCELLED, REJECTED, PARTIALLY_FILLED, FILLED)
- REST responses for clients (acknowledgements, synchronous cancel responses, status payloads)

Design & Operational Notes:
- Idempotency & correlation: require `clientOrderId` from brokers to allow safe retries; ensure server-side idempotency for order placement.
- Acknowledgement: respond quickly with `orderId` and accept asynchronous execution flow to keep gateway responsive.
- Synchronous cancel: implement a short timeout for the synchronous call to `ex-matching-engine` and return a clear error if the cancellation cannot be confirmed within that window.
- Security: authenticate and authorize broker clients; include broker-id in order metadata for later per-broker routing.
- Schema & validation: validate order fields (symbol, side, quantity, price, orderType) and return well-structured error messages.
- Observability: log incoming requests, forwarded messages, Liveness/Readiness probes, metrics for request rates, and latencies to `ex-matching-engine`.

Integration & Event Flow
- Place order: Broker -> `ex-order-manager` (REST) -> `exchange.orders.submitted` (Kafka) -> `ex-matching-engine` -> publishes execution events -> `ex-marketdata-distributor` delivers execution to broker.
- Cancel order: Broker -> `ex-order-manager` (REST synchronous) -> `ex-matching-engine` (sync call) -> response forwarded to broker and `exchange.orders.events` updated.
- Status query: Broker -> `ex-order-manager` (REST) -> read from `orderbook`/`executions` store or query `ex-matching-engine` if authoritative.

Security & Privacy:
- Ensure brokers can only operate on orders belonging to their broker-id; enforce scoping in APIs and events.

Demo Guidance:
- Provide example request/response JSON schemas for `POST /api/v1/orders`, `POST /api/v1/orders/{orderId}/cancel`, and `GET /api/v1/orders/{orderId}` in the service README.
