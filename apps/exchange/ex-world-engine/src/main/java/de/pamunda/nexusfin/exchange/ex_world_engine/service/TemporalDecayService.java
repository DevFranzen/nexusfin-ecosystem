package de.pamunda.nexusfin.exchange.ex_world_engine.service;

import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class TemporalDecayService {

    // Decay rate constants (λ - lambda)
    private static final double DECAY_RATE_HIGH_IMPACT = 0.1;   // Half-life ~7 hours
    private static final double DECAY_RATE_MEDIUM_IMPACT = 0.3; // Half-life ~2.3 hours
    private static final double DECAY_RATE_LOW_IMPACT = 0.7;    // Half-life ~1 hour

    private final VectorDbService vectorDbService;

    public TemporalDecayService(VectorDbService vectorDbService) {
        this.vectorDbService = vectorDbService;
    }

    /**
     * Retrieves recent news with temporal decay weights applied.
     *
     * @param now Current timestamp
     * @param primaryWindow Primary context window (e.g., last 2 hours)
     * @param secondaryWindow Secondary context window (e.g., last 24 hours) - can be null
     * @return List of weighted news items sorted by weight (descending)
     */
    public List<WeightedNews> getWeightedContext(
            LocalDateTime now,
            Duration primaryWindow,
            Duration secondaryWindow
    ) {
        LocalDateTime primaryStart = now.minus(primaryWindow);
        List<Document> recentNews;

        if (secondaryWindow != null) {
            LocalDateTime secondaryStart = now.minus(secondaryWindow);
            recentNews = vectorDbService.getNewsBetween(secondaryStart, now);
        } else {
            recentNews = vectorDbService.getRecentNews(primaryStart);
        }

        List<WeightedNews> weightedNews = new ArrayList<>();

        for (Document doc : recentNews) {
            NewsMetadata metadata = NewsMetadata.fromMap(doc.getMetadata());

            double weight = calculateDecayWeight(metadata, now);

            // Apply additional context window weight
            double contextWeight = calculateContextWindowWeight(
                    metadata.publication_timestamp(),
                    now,
                    primaryWindow,
                    secondaryWindow
            );

            double finalWeight = weight * contextWeight;
            weightedNews.add(new WeightedNews(
                    doc.getText(),
                    metadata,
                    finalWeight
            ));
        }

        // Sort by weight descending
        weightedNews.sort(Comparator.comparingDouble(WeightedNews::weight).reversed());

        // Normalize weights
        normalizeWeights(weightedNews);

        log.debug("Retrieved {} weighted news items for context window", weightedNews.size());

        return weightedNews;
    }

    /**
     * Calculates exponential decay weight based on news impact and age.
     * Formula: weight = e^(-λ * age_hours)
     */
    private double calculateDecayWeight(NewsMetadata metadata, LocalDateTime now) {
        double ageHours = Duration.between(metadata.publication_timestamp(), now).toMinutes() / 60.0;
        double lambda = getLambdaForImpact(metadata.impact_score());

        return Math.exp(-lambda * ageHours);
    }

    /**
     * Determines lambda (decay rate) based on impact score.
     */
    private double getLambdaForImpact(float impactScore) {
        if (impactScore >= 8.0) {
            return DECAY_RATE_HIGH_IMPACT;   // High impact: slow decay
        } else if (impactScore >= 5.0) {
            return DECAY_RATE_MEDIUM_IMPACT; // Medium impact: moderate decay
        } else {
            return DECAY_RATE_LOW_IMPACT;    // Low impact: fast decay
        }
    }

    /**
     * Calculates additional weight based on which context window the news falls into.
     */
    private double calculateContextWindowWeight(
            LocalDateTime newsTimestamp,
            LocalDateTime now,
            Duration primaryWindow,
            Duration secondaryWindow
    ) {
        Duration age = Duration.between(newsTimestamp, now);

        // Primary window: full weight (1.0)
        if (age.compareTo(primaryWindow) <= 0) {
            return 1.0;
        }

        // If no secondary window, news outside primary has minimal weight
        if (secondaryWindow == null) {
            return 0.1;
        }

        // Secondary window: reduced weight (0.3)
        if (age.compareTo(secondaryWindow) <= 0) {
            return 0.3;
        }

        // Outside all windows: minimal weight
        return 0.1;
    }

    /**
     * Normalizes weights so they sum to 1.0
     */
    private void normalizeWeights(List<WeightedNews> weightedNews) {
        if (weightedNews.isEmpty()) return;

        double sum = weightedNews.stream()
                .mapToDouble(WeightedNews::weight)
                .sum();

        if (sum > 0) {
            weightedNews.replaceAll(wn ->
                new WeightedNews(wn.content(), wn.metadata(), wn.weight() / sum)
            );
        }
    }

    /**
     * Record representing a news item with its calculated weight.
     */
    public record WeightedNews(
            String content,
            NewsMetadata metadata,
            double weight
    ) {}
}
