# Context — Broker: Order Gateway

Service: `br-order-gateway`
Location: `/apps/broker/br-order-gateway`
Role: Broker-facing gateway that accepts client order requests, validates and authorises them, and forwards them into the Broker backend for lifecycle processing. Acts as the canonical API boundary for client-facing order flows within the Broker system.

Responsibilities:
- Accept client order requests (place, cancel, status) via REST/OpenAPI endpoints and WebSocket for push notifications.
- Authenticate and authorise clients; include broker and client identity in order metadata for downstream scoping and routing.
- Validate request payloads and enforce business rules (symbol validity, quantity limits, order types).
- Forward validated orders to the Broker order processing pipeline (publish to `broker.orders.submitted` Kafka topic or call an internal `order-service` endpoint).
- Provide immediate acknowledgements for placed orders with an `orderId` and submission status; execution remains asynchronous and executions are delivered to clients via the Broker distribution stack (e.g., `br-marketdata-importer` -> Kafka -> client delivery).
- Support synchronous cancel requests by routing to the order processing component and returning success/failure synchronously when possible.
- Support order status queries (by `orderId`) reading from the Broker's order store or caching layer to allow client reconciliation.

Inputs:
- REST API: `POST /api/v1/orders` (place order), `POST /api/v1/orders/{orderId}/cancel` (cancel), `GET /api/v1/orders/{orderId}` (status)
- WebSocket: authenticated client connections for push notifications of acknowledgements and execution events
- Configuration/Admin: validation rules, rate-limits, allowed symbols per broker

Outputs:
- Kafka topics (Broker namespace):
  - `broker.orders.submitted` — validated order submissions for downstream processing
  - `broker.orders.events` — order lifecycle events (ACK, REJECT, CANCELLED, PARTIAL_FILL, FILL)
- REST responses to clients (acknowledgements, cancel confirmation, status payloads)
- WebSocket push messages for real-time updates to connected clients

Design & Operational Notes:
- Idempotency: require a `clientOrderId` or similar to support safe retries and deduplication.
- Validation: reject invalid orders with structured error payloads; support configurable validation rules per broker.
- Acknowledgement pattern: return a fast acknowledgement (`orderId`) to keep client latency low; do not block on execution.
- Cancellation: attempt synchronous cancellation where the order pipeline supports it; if not possible, return immediate ack and propagate cancellation attempt to the pipeline with eventual confirmation.
- Status queries: prefer serving from a read-optimized order store or cache; fall back to the authoritative order component when necessary.
- Delivery of executions: executions are not delivered by this gateway directly — they are emitted by the Broker execution pipeline and delivered via the Broker's distribution channel (per-broker) so that only the originating client/broker sees them.
- Observability: emit metrics for received orders/sec, validation errors, avg ack latency, cancel latency, and WebSocket connection counts; log request traces and correlation ids.

Security & Privacy:
- Enforce TLS for all client connections and require API keys or OAuth tokens for authentication.
- Authorize actions by broker/client scope; ensure clients cannot act on other clients' orders.
- Mask or avoid storing sensitive client data in logs.

Integration & Event Flow (example):
- Place order: Client -> `br-order-gateway` (REST) -> validate -> publish `broker.orders.submitted` -> `br-order-service` consumes -> processing -> execution events -> `broker.orders.events` -> delivery via `br-marketdata-importer`/distribution channels to the client.
- Cancel order: Client -> `br-order-gateway` (REST sync) -> forward to `br-order-service` -> respond with success/failure -> publish lifecycle event.
- Status query: Client -> `br-order-gateway` (REST) -> read from local cache/order-store -> return latest status.

Demo Guidance:
- Provide example request/response JSON for `POST /api/v1/orders`, `POST /api/v1/orders/{orderId}/cancel`, and `GET /api/v1/orders/{orderId}` in the service README.
- Include a small WebSocket client sample demonstrating connection, ack reception, and execution notifications.
