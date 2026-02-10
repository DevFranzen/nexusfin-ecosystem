# Context — Broker: Router (Cross-Landscape Gateway)

Service: `br-router`
Location: `/apps/broker/br-router`
Role: Single gateway service for ALL cross-landscape communication between Broker and Exchange systems. Acts as proxy for both inbound market data and outbound order requests.

Responsibilities:
- Establish and maintain connection to Exchange `ex-gateway`
- Proxy market data requests from `br-marketdata-importer` to Exchange
- Proxy order requests from `br-order-gateway` to Exchange
- Manage authentication with Exchange Keycloak (obtain and refresh JWT tokens)
- Handle connection failures, retries, and circuit breaking
- Provide observability for cross-landscape traffic

Inputs:
- Internal REST/WebSocket requests from:
  - `br-marketdata-importer` (requesting market data streams and backfills)
  - `br-order-gateway` (submitting orders, cancellations, status queries)
- Configuration: Exchange `ex-gateway` endpoint, Exchange Keycloak endpoint, retry policies, timeouts

Outputs:
- Proxied requests to Exchange `ex-gateway`
- Responses forwarded back to requesting Broker services
- WebSocket connections maintained for real-time market data streaming
- Metrics: Request rates, latencies, error rates, connection status

Design & Operational Notes:
- **Single point of external communication**: ONLY service in Broker landscape that knows Exchange location
- Authentication management:
  - Authenticate with Exchange Keycloak instance to obtain JWT tokens
  - Automatically refresh tokens before expiration
  - Attach valid tokens to all outbound requests to `ex-gateway`
- Request/response handling: Transformation logic to be defined in ADRs
- Circuit breaker: Detect Exchange unavailability and fail fast to prevent cascading failures
- Retry logic: Exponential backoff for failed requests (specifics in ADRs)
- WebSocket management: Maintain persistent WebSocket connections to `ex-gateway` for market data
- Observability: Track cross-landscape traffic, connection health, token refresh events

Security & Privacy:
- Secure storage of Exchange Keycloak credentials (client ID, client secret)
- TLS for all connections to Exchange
- No caching of sensitive data (orders, execution details)
- Log sanitization: Redact sensitive fields in request/response logs

Demo Guidance:
- Health check endpoint showing Exchange connectivity status
- Metrics dashboard showing cross-landscape traffic patterns
- Circuit breaker state visualization
- Pre-configured Exchange Keycloak client credentials