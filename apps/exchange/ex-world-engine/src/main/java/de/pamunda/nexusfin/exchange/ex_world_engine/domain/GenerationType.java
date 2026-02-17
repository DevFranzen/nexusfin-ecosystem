package de.pamunda.nexusfin.exchange.ex_world_engine.domain;

/**
 * Enumeration of news generation types.
 * Each type corresponds to a different prompt engineering strategy.
 */
public enum GenerationType {
    /**
     * Single atomic news item in a specific category.
     */
    ATOMIC,

    /**
     * Cluster of 3-5 related news items forming a thematic narrative.
     */
    CLUSTER,

    /**
     * Market summary aggregating last 2 hours of news.
     */
    SUMMARY,

    /**
     * Daily digest covering full trading day.
     */
    DIGEST
}
