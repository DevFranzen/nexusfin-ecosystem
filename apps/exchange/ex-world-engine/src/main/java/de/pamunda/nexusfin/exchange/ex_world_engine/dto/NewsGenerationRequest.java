package de.pamunda.nexusfin.exchange.ex_world_engine.dto;

import de.pamunda.nexusfin.exchange.ex_world_engine.domain.GenerationType;

import java.time.LocalDateTime;

/**
 * Request DTO passed through ChatClient context to NewsGenerationAdvisor.
 * Contains all parameters needed for prompt engineering strategies.
 *
 * @param type The type of news generation (ATOMIC, CLUSTER, SUMMARY, DIGEST)
 * @param category The news category (for ATOMIC news, null for others)
 * @param timestamp The publication timestamp
 * @param impactScore Impact level 1-10 (for ATOMIC news, 0 for others)
 * @param theme Thematic focus (for CLUSTER news, null for others)
 */
public record NewsGenerationRequest(
        GenerationType type,
        NewsCategory category,
        LocalDateTime timestamp,
        float impactScore,
        String theme
) {
    /**
     * Creates a request for atomic news generation.
     */
    public static NewsGenerationRequest forAtomicNews(
            NewsCategory category,
            LocalDateTime timestamp,
            float impactScore
    ) {
        return new NewsGenerationRequest(
                GenerationType.ATOMIC,
                category,
                timestamp,
                impactScore,
                null
        );
    }

    /**
     * Creates a request for news cluster generation.
     */
    public static NewsGenerationRequest forNewsCluster(
            NewsCategory primaryCategory,
            LocalDateTime timestamp,
            String theme
    ) {
        return new NewsGenerationRequest(
                GenerationType.CLUSTER,
                primaryCategory,
                timestamp,
                0,
                theme
        );
    }

    /**
     * Creates a request for market summary generation.
     */
    public static NewsGenerationRequest forMarketSummary(LocalDateTime timestamp) {
        return new NewsGenerationRequest(
                GenerationType.SUMMARY,
                null,
                timestamp,
                0,
                null
        );
    }

    /**
     * Creates a request for daily digest generation.
     */
    public static NewsGenerationRequest forDailyDigest(LocalDateTime timestamp) {
        return new NewsGenerationRequest(
                GenerationType.DIGEST,
                null,
                timestamp,
                0,
                null
        );
    }
}
