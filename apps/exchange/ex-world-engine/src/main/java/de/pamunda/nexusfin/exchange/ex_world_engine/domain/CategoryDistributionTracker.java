package de.pamunda.nexusfin.exchange.ex_world_engine.domain;

import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsCategory;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsMetadata;
import de.pamunda.nexusfin.exchange.ex_world_engine.service.VectorDbService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class CategoryDistributionTracker {

    private final Queue<CategoryTimestamp> recentCategories = new LinkedList<>();
    private final VectorDbService vectorDbService;

    // Category weights from plan: Core 60%, Peripheral 30%, Meta 10%
    private static final Map<NewsCategory, Double> BASE_WEIGHTS = Map.ofEntries(
            // Core Financial (60% total, 12% each for 5 categories)
            Map.entry(NewsCategory.MACRO_ECONOMICS, 0.12),
            Map.entry(NewsCategory.CORPORATE_EARNINGS, 0.12),
            Map.entry(NewsCategory.MERGERS_ACQUISITIONS, 0.12),
            Map.entry(NewsCategory.REGULATORY_POLICY, 0.12),
            Map.entry(NewsCategory.MARKET_STRUCTURE, 0.12),
            // Peripheral Impact (30% total, 7.5% each for 4 categories)
            Map.entry(NewsCategory.GEOPOLITICS, 0.075),
            Map.entry(NewsCategory.TECHNOLOGY, 0.075),
            Map.entry(NewsCategory.ENERGY_COMMODITIES, 0.075),
            Map.entry(NewsCategory.SOCIAL_SENTIMENT, 0.075),
            // Meta Categories (10% total, 10% for 1 category - SYNTHESIS excluded from atomic news)
            Map.entry(NewsCategory.ANALYST_COMMENTARY, 0.10)
    );

    public CategoryDistributionTracker(VectorDbService vectorDbService) {
        this.vectorDbService = vectorDbService;
    }

    /**
     * Initializes the tracker by loading recent news from Qdrant.
     * This ensures distribution enforcement works correctly after application restart.
     */
    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursAgo = now.minusHours(2);

        try {
            List<Document> recentNews = vectorDbService.getRecentNews(twoHoursAgo);

            for (Document doc : recentNews) {
                NewsMetadata metadata = NewsMetadata.fromMap(doc.getMetadata());
                // Only track atomic news, not summaries
                if (metadata.news_type() == NewsMetadata.Type.ATOMIC_NEWS) {
                    recentCategories.add(new CategoryTimestamp(
                            metadata.category(),
                            metadata.publication_timestamp()
                    ));
                }
            }

            log.info("Initialized CategoryDistributionTracker with {} recent news items", recentCategories.size());

        } catch (Exception e) {
            log.warn("Failed to initialize CategoryDistributionTracker from Qdrant, starting with empty state", e);
        }
    }

    /**
     * Selects the next news category based on distribution enforcement rules.
     *
     * Rules:
     * - No single category > 40% of hourly news
     * - Each core category appears at least once every 2 hours
     * - Weighted random selection with adjustments
     */
    public NewsCategory selectNextCategory(LocalDateTime now) {
        cleanOldEntries(now);

        Map<NewsCategory, Double> adjustedWeights = calculateAdjustedWeights(now);

        NewsCategory selected = weightedRandomSelection(adjustedWeights);

        // Track the selection
        recentCategories.add(new CategoryTimestamp(selected, now));

        log.debug("Selected category {} with adjusted weight {}", selected, adjustedWeights.get(selected));

        return selected;
    }

    /**
     * Removes entries older than 2 hours to maintain sliding window.
     */
    private void cleanOldEntries(LocalDateTime now) {
        LocalDateTime twoHoursAgo = now.minusHours(2);
        recentCategories.removeIf(ct -> ct.timestamp.isBefore(twoHoursAgo));
    }

    /**
     * Calculates adjusted weights based on recent distribution.
     */
    private Map<NewsCategory, Double> calculateAdjustedWeights(LocalDateTime now) {
        Map<NewsCategory, Double> weights = new HashMap<>(BASE_WEIGHTS);

        if (recentCategories.isEmpty()) {
            return weights;
        }

        // Calculate distribution in last hour
        Map<NewsCategory, Long> hourlyDistribution = getHourlyDistribution(now);
        long totalInLastHour = hourlyDistribution.values().stream().mapToLong(Long::longValue).sum();

        // Get categories not seen in last 2 hours
        Set<NewsCategory> absentCategories = getCategoriesAbsentInLast2Hours(now);

        for (NewsCategory category : BASE_WEIGHTS.keySet()) {
            double baseWeight = BASE_WEIGHTS.get(category);
            long count = hourlyDistribution.getOrDefault(category, 0L);

            if (totalInLastHour > 0) {
                double percentage = (double) count / totalInLastHour;

                // Block if > 40% of last hour
                if (percentage > 0.40) {
                    weights.put(category, 0.0);
                    log.debug("Blocking category {} ({}% of last hour exceeds 40%)", category, percentage * 100);
                    continue;
                }

                // Reduce weight by 50% if > 30% of last hour
                if (percentage > 0.30) {
                    weights.put(category, baseWeight * 0.5);
                    log.debug("Reducing weight for category {} ({}% of last hour)", category, percentage * 100);
                    continue;
                }
            }

            // Boost weight by 2x if absent in last 2 hours (only for core categories)
            if (absentCategories.contains(category) && isCoreCategory(category)) {
                weights.put(category, baseWeight * 2.0);
                log.debug("Boosting weight for category {} (absent in last 2 hours)", category);
            }
        }

        return weights;
    }

    /**
     * Gets count of each category in the last hour.
     */
    private Map<NewsCategory, Long> getHourlyDistribution(LocalDateTime now) {
        LocalDateTime oneHourAgo = now.minusHours(1);

        Map<NewsCategory, Long> distribution = new HashMap<>();

        for (CategoryTimestamp ct : recentCategories) {
            if (ct.timestamp.isAfter(oneHourAgo)) {
                distribution.merge(ct.category, 1L, Long::sum);
            }
        }

        return distribution;
    }

    /**
     * Gets categories that haven't appeared in the last 2 hours.
     */
    private Set<NewsCategory> getCategoriesAbsentInLast2Hours(LocalDateTime now) {
        LocalDateTime twoHoursAgo = now.minusHours(2);

        Set<NewsCategory> presentCategories = new HashSet<>();
        for (CategoryTimestamp ct : recentCategories) {
            if (ct.timestamp.isAfter(twoHoursAgo)) {
                presentCategories.add(ct.category);
            }
        }

        Set<NewsCategory> absentCategories = new HashSet<>(BASE_WEIGHTS.keySet());
        absentCategories.removeAll(presentCategories);

        return absentCategories;
    }

    /**
     * Checks if a category is a core financial category.
     */
    private boolean isCoreCategory(NewsCategory category) {
        return switch (category) {
            case MACRO_ECONOMICS, CORPORATE_EARNINGS, MERGERS_ACQUISITIONS,
                 REGULATORY_POLICY, MARKET_STRUCTURE -> true;
            default -> false;
        };
    }

    /**
     * Performs weighted random selection from the adjusted weights.
     */
    private NewsCategory weightedRandomSelection(Map<NewsCategory, Double> weights) {
        double totalWeight = weights.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (totalWeight == 0) {
            // All categories blocked, fall back to base weights
            log.warn("All categories blocked, falling back to base weights");
            return weightedRandomSelection(BASE_WEIGHTS);
        }

        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0.0;

        for (Map.Entry<NewsCategory, Double> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (random <= cumulative) {
                return entry.getKey();
            }
        }

        // Fallback (should never reach here)
        return weights.keySet().iterator().next();
    }

    /**
     * Record to track category and timestamp.
     */
    private record CategoryTimestamp(NewsCategory category, LocalDateTime timestamp) {}
}
