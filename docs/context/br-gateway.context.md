# Context — Broker: Gateway (API Gateway)

Service: `br-gateway`
Location: `/apps/broker/br-gateway`
Role: External-facing API Gateway for the Broker landscape, providing unified entry point, authentication, rate limiting, and routing to internal Broker services.

Responsibilities:
- Route external requests to internal Broker services
- Handle authentication and authorization via Keycloak integration
- Apply rate limiting and quota enforcement per client
- Request logging, correlation ID injection, and distributed tracing
- Provide health checks and API documentation endpoints

Inputs:
- External HTTP/HTTPS requests from client applications (web, mobile, API consumers)
- Keycloak: Token validation and user/client authentication
- Configuration: routing rules, rate limits

Outputs:
- Proxied requests to internal Broker services:
  - `/api/v1/orders/*` → `br-order-gateway`
  - `/api/v1/portfolios/*` → `br-portfolio-manager`
  - `/api/v1/accounts/*` → `br-account-service`
  - `/api/v1/marketdata/*` → `br-marketdata-store`
- HTTP responses to external clients
- WebSocket upgrade for real-time order updates (routed to `br-order-gateway`)
- Access logs with correlation IDs

Design & Operational Notes:
- Gateway implementation: Spring Cloud Gateway
- Authentication: JWT token validation via Keycloak
- Rate limiting: Per-client quotas (specifics to be defined in ADRs)
- CORS configuration: To be defined in ADRs based on client requirements
- WebSocket support: Upgrade connections and route to `br-order-gateway` for live updates
- Observability: Distributed tracing, access logs, authentication metrics

Security & Privacy:
- Enforce TLS 1.2+ for all external connections
- Validate JWT tokens with Keycloak before routing requests
- Input validation and sanitization to prevent injection attacks
- Log security events (authentication failures, rate limit violations)

Demo Guidance:
- Provide example authenticated requests for each route
- Document token acquisition flow from Keycloak
- Show WebSocket connection upgrade example