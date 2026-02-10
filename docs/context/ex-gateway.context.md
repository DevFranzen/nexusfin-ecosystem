# Context — Exchange: Gateway (API Gateway)

Service: `ex-gateway`
Location: `/apps/exchange/ex-gateway`
Role: External-facing API Gateway for the Exchange landscape, providing single entry point, authentication, rate limiting, and routing to internal Exchange services.

Responsibilities:
- Route external requests to internal Exchange services
- Validate JWT tokens with Keycloak before routing
- Apply rate limiting and quota enforcement per broker client
- Request logging, correlation ID injection, and distributed tracing
- Provide health checks and API documentation endpoints

Inputs:
- External HTTP/HTTPS requests from Broker `br-router`
- Keycloak: Token validation and broker client authentication
- Configuration: routing rules, rate limits

Outputs:
- Proxied requests to internal Exchange services:
  - `/api/v1/orders/*` → `ex-order-manager`
  - `/api/v1/marketdata/*` → `ex-marketdata-distributor`
  - `/api/v1/companies/*` → `ex-matching-engine` (company management)
  - `/api/v1/news/*` → `ex-world-engine`
- HTTP responses to Broker clients
- WebSocket upgrade for market data streaming (routed to `ex-marketdata-distributor`)
- Access logs with correlation IDs

Design & Operational Notes:
- Gateway implementation: Spring Cloud Gateway
- Authentication: JWT token validation via Keycloak
- Rate limiting: Per-broker quotas (specifics to be defined in ADRs)
- WebSocket support: Upgrade connections and route to `ex-marketdata-distributor` for live market data
- Single entry point: ALL external access to Exchange MUST go through this gateway
- Internal services are not directly accessible from outside the Exchange landscape
- Observability: Distributed tracing, access logs, authentication metrics

Security & Privacy:
- Enforce TLS 1.2+ for all external connections
- Validate JWT tokens with Keycloak before routing requests
- Input validation and sanitization to prevent injection attacks
- Log security events (authentication failures, rate limit violations)
- Broker-scoped authorization: Ensure brokers can only access their own data

Demo Guidance:
- Provide example authenticated requests for each route
- Document broker token acquisition flow from Keycloak
- Show WebSocket connection upgrade example for market data streaming