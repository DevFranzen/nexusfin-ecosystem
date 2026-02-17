package de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.strategy;

import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsMetadata;

/**
 * Result of news generation containing both metadata and full content.
 *
 * @param metadata The news metadata (title, category, sentiment, etc.)
 * @param content The full news content text
 */
public record NewsGenerationResult(
        NewsMetadata metadata,
        String content
) {
}
