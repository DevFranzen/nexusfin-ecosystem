# Context — Broker: Account Service

Service: `br-account-service`
Location: `/apps/broker/br-account-service`
Role: Broker-side service for managing client accounts and account lifecycle operations.

Responsibilities:
- Create, update, and deactivate client accounts
- Store and manage account metadata
- Expose REST APIs for account CRUD operations and queries
- Publish account lifecycle events to Kafka for downstream services

Inputs:
- REST API: Account management requests from `br-gateway`
  - `POST /api/v1/accounts` (create account)
  - `PATCH /api/v1/accounts/{accountId}` (update)
  - `GET /api/v1/accounts/{accountId}` (retrieve)
  - `DELETE /api/v1/accounts/{accountId}` (deactivate)

Outputs:
- Database: `accounts` table (schema details to be defined in ADRs)
- Kafka topic: `broker.accounts.events` (CREATED, UPDATED, DEACTIVATED)
- REST responses: Account details, creation confirmations

Design & Operational Notes:
- Data model: Relational store (PostgreSQL) for transactional consistency
- Account field definitions: To be defined in ADRs
- Authentication: Handled by Keycloak via `br-gateway` (separate Keycloak instance for Broker landscape)
- Idempotency: Support `clientRequestId` for account creation to allow safe retries
- Account deactivation: Soft delete (status = INACTIVE) to preserve audit trail
- Downstream integration: `br-portfolio-manager` subscribes to `broker.accounts.events` to initialize portfolios when accounts are created
- Observability: Track account creation rates, query latencies

Security & Privacy:
- PII handling: Synthesize all personal data for demo purposes; no real PII
- Authentication and authorization delegated to Keycloak
- Secrets management: Never commit credentials to repository

Demo Guidance:
- Provide sample account creation request/response
- Pre-seed demo accounts for testing
- Show integration with portfolio initialization