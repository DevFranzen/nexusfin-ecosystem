# Context — Broker: Order Gateway

Service: `br-order-gateway`
Location: `/apps/broker/br-order-gateway`
Role: Broker-facing gateway that accepts client order requests, validates and authorizes them, and forwards them to Exchange via `br-router`. Acts as the canonical API boundary for client-facing order flows within the Broker system.

Responsibilities:
- Accept client order requests (place, cancel, status) via REST/OpenAPI endpoints and WebSocket for push notifications
- Authenticate and authorize clients via `br-gateway` (Keycloak integration)
- Validate request payloads and enforce business rules
- Forward validated orders to Exchange via `br-router` proxy
- Provide immediate acknowledgements for placed orders with an `orderId` and submission status
- Support synchronous cancel requests by routing to Exchange via `br-router`
- Support order status queries (by `orderId`) reading from the Broker's order store or cache

Inputs:
- REST API (via `br-gateway`):
  - `POST /api/v1/orders` (place order)
  - `POST /api/v1/orders/{orderId}/cancel` (cancel)
  - `GET /api/v1/orders/{orderId}` (status)
- WebSocket: Authenticated client connections for push notifications
- Configuration: Validation rules, rate limits, allowed symbols

Outputs:
- REST requests to Exchange (via `br-router` proxy)
- Kafka topics (Broker namespace):
  - `broker.orders.events` — order lifecycle events (ACK, REJECT, CANCELLED, PARTIAL_FILL, FILL)
- REST responses to clients (acknowledgements, cancel confirmation, status payloads)
- WebSocket push messages for real-time updates to connected clients

Design & Operational Notes:
- **Architecture**: Orders are forwarded to Exchange `ex-order-manager` via `br-router` proxy (never direct connection)
- Validation rules: To be defined in ADRs (symbol validation, quantity limits, order types)
- WebSocket event types: To be defined in ADRs (acknowledgements, fills, cancellations)
- Idempotency: Require `clientOrderId` for safe retries and deduplication
- Acknowledgement pattern: Return fast acknowledgement (`orderId`) to keep client latency low; execution is asynchronous
- Cancellation: Forward to Exchange via `br-router`; return synchronous success/failure
- Status queries: Serve from local order store/cache when possible
- Observability: Metrics for received orders/sec, validation errors, ack latency, cancel latency, WebSocket connection counts

Security & Privacy:
- Authentication/authorization handled by `br-gateway` (Keycloak)
- Authorize actions by client scope; ensure clients cannot act on other clients' orders
- Mask sensitive client data in logs

Integration & Event Flow:
- Place order: Client → `br-gateway` → `br-order-gateway` → validate → `br-router` → Exchange `ex-order-manager` → execution → execution events flow back via `br-marketdata-importer`
- Cancel order: Client → `br-gateway` → `br-order-gateway` → `br-router` → Exchange `ex-order-manager` → synchronous response
- Status query: Client → `br-gateway` → `br-order-gateway` → read from local cache/order-store → return status

Demo Guidance:
- Provide example request/response JSON for order operations
- Include WebSocket client sample demonstrating connection and event reception