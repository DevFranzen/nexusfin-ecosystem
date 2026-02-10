# Context — Broker: Infrastructure

Service: `br-infra`
Location: `/apps/broker/br-infra`
Role: Infrastructure-as-code and deployment configurations for Broker landscape shared infrastructure (Kafka, databases, authentication, observability stack).

Responsibilities:
- Define and manage Broker Kafka cluster configuration (brokers, topics, replication, retention)
- Configure and deploy shared databases (PostgreSQL for transactional data, time-series DB for tick storage)
- Provide authentication service (Keycloak) for token-based API access
- Set up observability infrastructure for monitoring and debugging
- Provide Docker Compose configurations for local development and demo environments
- Document infrastructure dependencies and startup order

Infrastructure Components:

1. **Kafka Cluster** (Broker namespace)
   - Topics: `broker.market.ticks.*`, `broker.market.executions`, `broker.orders.*`, `broker.portfolios.events`, `broker.accounts.events`
   - Configuration details (replication factor, retention policies) to be defined in ADRs

2. **Databases**
   - PostgreSQL: Accounts, orders, portfolios, positions, ledger
   - TimescaleDB/InfluxDB: Time-series tick data storage
   - Schema migration tooling (Flyway/Liquibase)

3. **Authentication Service**
   - Keycloak: Token-based authentication and authorization for client applications
   - OAuth2/JWT token issuance for end-user clients
   - User/client management and realm configuration

4. **Observability**
   - Observability stack choices and configuration deferred to ADRs
   - Requirements: metrics collection, log aggregation, service health monitoring

5. **Development Environment**
   - Docker Compose file for running entire Broker landscape
   - Service dependency orchestration and health checks

Outputs:
- Docker Compose file: `apps/broker/br-infra/docker-compose.yml`
- Kafka topic definitions: `kafka-topics.yaml`
- Database schemas: `schema/migrations/*.sql`
- Keycloak realm configuration: `keycloak-realm.json`
- Documentation: Setup and troubleshooting guides

Design & Operational Notes:
- Kafka topic naming: Enforce `broker.*` namespace prefix for all topics
- Topic retention policies: To be defined in ADRs based on demo requirements
- Network isolation: Broker infrastructure is completely isolated from Exchange landscape except for the designated gateway service
- Startup order:
  1. Kafka cluster
  2. Databases (PostgreSQL, TimescaleDB/InfluxDB)
  3. Keycloak (authentication service)
  4. Observability stack
  5. `br-router` (single gateway to Exchange system)
  6. Other Broker services (`br-marketdata-importer`, `br-order-gateway`, etc.)
- Cross-landscape communication (single gateway pattern):
  - **ONLY `br-router`** knows how to reach the Exchange system
  - `br-marketdata-importer` uses `br-router` as proxy to receive market data and execution events from Exchange
  - `br-order-gateway` uses `br-router` as proxy to send orders and requests to Exchange
  - All other Broker services communicate exclusively within the Broker Kafka namespace and internal APIs
  - This ensures a single, controlled point of external communication for security and maintainability

Security & Privacy:
- Kafka authentication: Configuration to be defined in ADRs
- Database encryption: To be defined in ADRs
- Secrets management: Use environment variables; never commit secrets to repository
- PII handling: Synthesize all personal data for demo purposes; no real PII

Demo Guidance:
- Single-command startup for local development environment
- Health check endpoints for verifying service availability
- Sample data seeding scripts for demo scenarios
- Keycloak pre-configured with demo client credentials