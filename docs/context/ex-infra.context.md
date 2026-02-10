# Context — Exchange: Infrastructure

Service: `ex-infra`
Location: `/apps/exchange/ex-infra`
Role: Infrastructure-as-code and deployment configurations for Exchange landscape shared infrastructure (Kafka, databases, authentication, observability stack).

Responsibilities:
- Define and manage Exchange Kafka cluster configuration (brokers, topics, replication, retention)
- Configure and deploy shared databases (PostgreSQL for transactional data, Qdrant for news storage)
- Provide authentication service (Keycloak) for token-based API access
- Set up observability infrastructure for monitoring and debugging
- Provide Docker Compose configurations for local development and demo environments
- Document infrastructure dependencies and startup order

Infrastructure Components:

1. **Kafka Cluster** (Exchange namespace)
   - Topics: `exchange.market.*.ticks`, `exchange.trades.executed`, `exchange.orders.*`, `exchange.companies.events`
   - Configuration details (replication factor, retention policies) to be defined in ADRs

2. **Databases**
   - PostgreSQL: Company registry, orderbook, executions, order ledger
   - Qdrant: Vector database for news items and embeddings (used by `ex-world-engine`)
   - Schema migration tooling (Flyway/Liquibase)

3. **Authentication Service**
   - Keycloak: Token-based authentication and authorization
   - OAuth2/JWT token issuance for Broker clients
   - User/client management and realm configuration

4. **Observability**
   - Observability stack choices and configuration deferred to ADRs
   - Requirements: metrics collection, log aggregation, service health monitoring

5. **Development Environment**
   - Docker Compose file for running entire Exchange landscape
   - Service dependency orchestration and health checks

Outputs:
- Docker Compose file: `apps/exchange/ex-infra/docker-compose.yml`
- Kafka topic definitions: `kafka-topics.yaml`
- Database schemas: `schema/migrations/*.sql`
- Keycloak realm configuration: `keycloak-realm.json`
- Documentation: Setup and troubleshooting guides

Design & Operational Notes:
- Kafka topic naming: Enforce `exchange.*` namespace prefix for all topics
- Topic retention policies: To be defined in ADRs based on demo requirements
- Network isolation: Exchange infrastructure is completely isolated from Broker landscape
- Startup order:
  1. Kafka cluster
  2. Databases (PostgreSQL, Qdrant)
  3. Keycloak (authentication service)
  4. `ex-world-engine` (LLM-driven news generation)
  5. `ex-matching-engine` (price generation and orderbook)
  6. Internal services (`ex-order-manager`, `ex-marketdata-distributor`)
  7. `ex-gateway` (Spring Cloud Gateway - single entry point to Exchange)
- API Gateway Pattern:
  - **`ex-gateway` is the ONLY official entry point** to the Exchange system landscape
  - All external requests (from Broker `br-router`) must go through `ex-gateway`
  - `ex-gateway` validates tokens with Keycloak before routing to internal services
  - Internal Exchange services are not directly accessible from outside
- Authentication Flow:
  - Broker clients authenticate with Keycloak to obtain JWT tokens
  - Tokens are passed in requests to `ex-gateway`
  - `ex-gateway` validates tokens and routes to appropriate internal service
  - Token validation is centralized at the gateway layer

Security & Privacy:
- Kafka authentication: Configuration to be defined in ADRs
- Database encryption: To be defined in ADRs
- Secrets management: Use environment variables; never commit secrets to repository
- API authentication: All external access requires valid Keycloak JWT tokens
- Token-based authorization: Keycloak manages broker client permissions and scopes

Demo Guidance:
- Single-command startup for local development environment
- Health check endpoints for verifying service availability
- Sample data seeding scripts for demo scenarios (companies, initial prices)
- Keycloak pre-configured with demo broker client credentials