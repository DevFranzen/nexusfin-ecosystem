# Context — Exchange: Company Registry

Service: `ex-company-registry`
Location: `/apps/exchange/ex-company-registry`
Role: Authoritative registry for all companies listed on the Exchange, providing company metadata, listing/delisting operations, and search APIs.

Responsibilities:
- Maintain the master registry of listed companies with metadata (symbol, name, sector, listing date, background story)
- Provide REST API for company listing and delisting operations
- Generate company background stories using LLM based on seed parameters during listing
- Expose search and filter APIs for company discovery (by sector, symbol, listing status)
- Publish company lifecycle events to Kafka for downstream consumers

Inputs:
- REST API: `POST /api/v1/companies` (list new company with seed params)
- REST API: `DELETE /api/v1/companies/{symbol}` (delist company)
- REST API: `GET /api/v1/companies/{symbol}` (retrieve company details)
- REST API: `GET /api/v1/companies?sector={}&status={}` (search/filter)
- LLM API: For generating company background stories

Outputs:
- Database: `companies` table (symbol, name, sector, listing_date, status, background_story, metadata)
- Kafka topic: `exchange.companies.events` (LISTED, DELISTED, UPDATED)
- REST API responses: Company details, listing confirmations

Design & Operational Notes:
- Company listing flow: Accept seed parameters → call LLM to generate background story → persist → publish LISTED event
- LLM prompt: Include sector, company size, founding year to generate coherent background for market simulation
- Downstream integration: `ex-matching-engine` subscribes to `exchange.companies.events` to initialize price generation for newly listed companies
- Validation: Ensure symbol uniqueness and format compliance (uppercase, alphanumeric, max 10 chars)
- Delisting: Soft delete (status = DELISTED) to preserve historical data
- Observability: Track listing/delisting rates, LLM generation latencies, query QPS

Security & Privacy:
- Admin-only access for listing/delisting operations via `ex-gateway`
- Synthesize all personal details in background stories; no real PII

Demo Guidance:
- Provide sample listing request with seed parameters
- Show LLM-generated background story example
- Pre-seed demo companies for testing