# Context — Exchange: World Engine

Service: `ex-world-engine`
Location: `/apps/exchange/ex-world-engine`
Role: LLM-driven market world engine that (1) generates news items by category, (2) aggregates those news items into concise market/ country-state summaries, and (3) publishes scenario events and market ticks for downstream consumers.

Responsibilities:
- Produce periodic news items organized by topical categories (e.g., macro, sector, company, policy, geopolitical). News production precedes real-time tick generation.
- Aggregate produced news into higher-level market and country-state summaries ("market state") that capture whether a significant event occurred and which sector is affected. The determination of "significant" and affected sector is implemented as a controlled random/ stochastic decision for demo purposes.
- Use an LLM prompt to generate the natural-language content of each news item; include context from recent news and summaries to keep items coherent.
- Store news items and aggregated summaries in a vector database to enable contextual retrieval.
- Expose an API for other services to query recent news, summaries, and vector-search results for use as input to price simulation.
- Use stored news as additional context when prompting the LLM to generate new news items so that all news remains contextually coherent.

Inputs:
- Scenario control inputs and scheduling (e.g., frequency, categories to emphasize).
- Optional seed events or injected headlines (via admin API).
- Vector DB retrievals (recent news embeddings) used as context when composing prompts.

Outputs:
- Persisted records in a vector database: news items, summaries, and associated embeddings.
- REST API endpoints for retrieving news, summaries, and performing semantic search over news.

Design & Operational Notes:
- News-generation flow: (1) retrieve recent context via vector DB, (2) build LLM prompt including retrieved context + scenario controls, (3) call LLM to generate news text and metadata (category, severity), (4) persist text + embedding, (5) provide news event and aggregated summaries as API.
- Significance and sector assignment are intentionally stochastic for demo reproducibility; document the randomness parameters in service config to allow repeatable runs.
- Vector DB should support fast semantic search (e.g., Milvus, Pinecone, Qdrant) and store embeddings alongside original text and metadata.
- Ensure prompts, prompt templates, and LLM parameters are stored in configuration and versioned to allow reproducibility of generated outputs.
- Provide observability: logs for generated prompts/responses, metrics for news frequency, vector DB queries, and API request rates.

Security & Privacy:
- Do not store or expose any sensitive personal data in news items; redact or synthesize personal information where needed.

Demo Guidance:
- The API exists so other exchange components or Broker services can retrieve news to influence price simulation or to display in UIs.
