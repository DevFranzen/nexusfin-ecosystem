# ex-infra: Exchange Infrastructure Stack

Complete containerized infrastructure for the NexusFin Exchange ecosystem, providing databases, event streaming, monitoring, logging, authentication, vector search, and AI/LLM capabilities.

## Overview

The Exchange Infrastructure (`ex-infra`) provides all foundational services required by Exchange microservices. This stack is composed of 11 containerized services organized into functional categories.

## Infrastructure Stack

### 1. **Data Layer**

#### PostgreSQL (`postgres`)
- **Purpose:** Transactional database for orders, accounts, audit logs, and operational data
- **Ports:** `5432:5432` (Database API)
- **Type:** Database API
- **.env Configuration:**
  - `POSTGRES_USER` - Database user (default: `user`)
  - `POSTGRES_PASSWORD` - Database password
  - `POSTGRES_DB` - Database name (default: `mydb`)
  - `POSTGRES_MEMORY_LIMIT` - Memory limit (default: `256M`)
- **Also used by:** Keycloak for authentication data

#### PostgreSQL Exporter (`postgres-exporter`)
- **Purpose:** Prometheus metrics exporter for PostgreSQL monitoring
- **Type:** Metrics API (internal)
- **.env Configuration:**
  - Inherits PostgreSQL credentials from main DB config
- **Exports to:** Prometheus

#### Qdrant Vector Database (`qdrant`)
- **Purpose:** Vector search database for semantic news retrieval and embeddings
- **Ports:**
  - `6333:6333` (REST API)
  - `6334:6334` (gRPC API)
- **Type:** Vector Search API
- **.env Configuration:**
  - `QDRANT_MEMORY_LIMIT` - Memory limit (default: `512M`)

---

### 2. **Event Streaming & Message Broker**

#### Redpanda (`redpanda`) + Console UI
- **Purpose:** Kafka-compatible distributed event streaming broker for real-time market data, order execution events, and trade notifications
- **Ports:**
  - `9092:9092` - Kafka API (External - for microservices)
  - `8081:8081` - Schema Registry API
  - `8082:8082` - Pandaproxy (HTTP Proxy API)
  - `9644:9644` - Admin API (for monitoring/rpk)
  - `8088:8080` - **Redpanda Console UI** (Web dashboard)
- **Type:** Event Streaming API + **Web UI**
- **.env Configuration:**
  - `REDPANDA_NODE_ID` - Node identifier (default: `0`)
  - `REDPANDA_SMP` - CPU cores allocated (default: `1`)
  - `REDPANDA_MEMORY` - Memory allocation (default: `512M`)
  - `REDPANDA_RESERVE_MEMORY` - Reserved memory (default: `0M`)
- **Kafka Topics (Exchange):**
  - `exchange.market.ticks` - Price ticks
  - `exchange.orders.submitted` - Incoming orders
  - `exchange.trades.executed` - Trade executions

---

### 3. **Monitoring & Observability**

#### Prometheus (`prometheus`)
- **Purpose:** Time-series metrics database and monitoring system
- **Ports:** `9090:9090` (Metrics API)
- **Type:** Metrics API
- **.env Configuration:**
  - `PROMETHEUS_MEMORY_LIMIT` - Memory limit (default: `256M`)
- **Scrapes metrics from:**
  - PostgreSQL Exporter
  - Qdrant
  - Redpanda
  - Application JVM metrics

#### Grafana (`grafana`)
- **Purpose:** Metrics visualization and dashboarding
- **Ports:** `3000:3000`
- **Type:** **Web UI**
- **.env Configuration:**
  - `GF_SECURITY_ADMIN_PASSWORD` - Admin password
  - `GRAFANA_MEMORY_LIMIT` - Memory limit (default: `256M`)
- **Default Access:** `http://localhost:3000` (Username: `admin`)

#### Loki (`loki`)
- **Purpose:** Log aggregation and storage system
- **Ports:** `3100:3100` (Logs API)
- **Type:** Logging API
- **.env Configuration:**
  - `LOKI_MEMORY_LIMIT` - Memory limit (default: `256M`)

#### Promtail (`promtail`)
- **Purpose:** Log collector agent - streams Docker container logs to Loki
- **Type:** Log Collector (internal)
- **.env Configuration:** Inherits from global config
- **Data source:** Docker container logs at `/var/lib/docker/containers`

---

### 4. **Authentication & Authorization**

#### Keycloak (`keycloak`)
- **Purpose:** OAuth2 / OpenID Connect identity provider and access management
- **Ports:** `8080:8080` (Auth API + **Web Admin Console UI**)
- **Type:** Auth API + **Web UI**
- **.env Configuration:**
  - `KC_DB` - Database type (default: `postgres`)
  - `KC_DB_URL` - Database connection URL
  - `KC_DB_USERNAME` - DB user credentials
  - `KC_DB_PASSWORD` - DB password
  - `KEYCLOAK_ADMIN` - Admin username (default: `admin`)
  - `KEYCLOAK_ADMIN_PASSWORD` - Admin password
  - `KEYCLOAK_MEMORY_LIMIT` - Memory limit (default: `750M`)
- **Default Access:** `http://localhost:8080` (Admin console at `/admin`)

---

### 5. **AI/LLM Services**

#### Ollama (`ollama`)
- **Purpose:** Local LLM inference engine for embedding models and AI-driven market dynamics
- **Ports:** `11434:11434` (Ollama API)
- **Type:** AI/LLM API
- **.env Configuration:**
  - `AI_MODEL` - Embedding model name (default: `embeddinggemma`)
- **Used by:** `ex-world-engine` for news generation and `ex-matching-engine` for GBM parameter derivation
- **Model loaded on startup:** Automatically pulls and loads the specified embedding model

---

## Service Summary Table

| Service | Category | Ports | Type | Purpose |
|---------|----------|-------|------|---------|
| **PostgreSQL** | Data | `5432` | DB API | Transactional data & audit logs |
| **Qdrant** | Data | `6333, 6334` | Vector Search | News embeddings & semantic search |
| **Redpanda** | Messaging | `9092, 8081, 8082, 9644` | Kafka API | Event streaming & topic management |
| **Redpanda Console** | Messaging | `8088` | **Web UI** | Kafka topic exploration & monitoring |
| **Prometheus** | Monitoring | `9090` | Metrics API | Time-series metrics storage |
| **Grafana** | Monitoring | `3000` | **Web UI** | Metrics dashboards & alerts |
| **Loki** | Logging | `3100` | Logs API | Log aggregation |
| **Promtail** | Logging | - | Collector | Ships container logs to Loki |
| **Keycloak** | Auth | `8080` | Auth API + **Web UI** | OAuth2/OIDC identity provider |
| **Ollama** | AI/LLM | `11434` | LLM API | Embedding model inference |
| **PG Exporter** | Monitoring | - | Metrics | PostgreSQL → Prometheus |

---

## Quick Start

### 1. Configure Environment
```bash
# Edit .env to set passwords and resource limits
nano .env
```

### 2. Start All Services
```bash
docker-compose up -d
```

### 3. Access Web UIs
- **Grafana** → `http://localhost:3000` (admin/your-password)
- **Keycloak** → `http://localhost:8080` (admin/your-password)
- **Redpanda Console** → `http://localhost:8088`

### 4. Verify Services
```bash
docker-compose ps
```

---

## API Endpoints by Category

### **Web UIs (Browsable Dashboards)**
- Grafana: `http://localhost:3000`
- Keycloak Admin: `http://localhost:8080/admin`
- Redpanda Console: `http://localhost:8088`
- Qdrant Dashboard: `http://localhost:6333/dashboard`

### **Kafka/Event Streaming**
- Kafka API: `localhost:9092` (for microservices)
- Schema Registry: `http://localhost:8081`
- Pandaproxy: `http://localhost:8082`
- Admin API: `http://localhost:9644`

### **Databases**
- PostgreSQL: `localhost:5432`
- Qdrant REST: `http://localhost:6333`
- Qdrant gRPC: `localhost:6334`

### **Monitoring & Logging**
- Prometheus: `http://localhost:9090`
- Loki: `http://localhost:3100`

### **AI/LLM**
- Ollama: `http://localhost:11434`

---

## Resource Requirements

Total memory allocation (configurable in `.env`):
- PostgreSQL: 256M
- Qdrant: 512M
- Redpanda: 512M
- Prometheus: 256M
- Grafana: 256M
- Loki: 256M
- Keycloak: 750M
- Ollama: ~2-4GB (depends on model)

**Minimum recommended:** 6GB RAM for local development

---

## Configuration Notes

- All services read configuration from the root `.env` file
- Health checks are configured for critical services (PostgreSQL, Redpanda, Ollama)
- Prometheus auto-discovers metrics from Redpanda, PostgreSQL, and Qdrant exporters
- Grafana is pre-configured to use Prometheus as a data source
- Promtail automatically streams container logs to Loki

---

## Dependency Chain

```
PostgreSQL ← Keycloak, Applications
    ↓
PG Exporter → Prometheus
    ↓         ↓
Qdrant ──→ Prometheus
    ↓         ↓
Redpanda ─→ Prometheus → Grafana
    ↓
Console (Web UI)

Promtail → Loki

Ollama (standalone, used by ex-world-engine & ex-matching-engine)
```
