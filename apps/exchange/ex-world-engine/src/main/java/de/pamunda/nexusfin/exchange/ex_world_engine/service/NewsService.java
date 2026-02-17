package de.pamunda.nexusfin.exchange.ex_world_engine.service;

import de.pamunda.nexusfin.exchange.ex_world_engine.domain.TimeOfDay;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsCategory;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsGenerationRequest;
import de.pamunda.nexusfin.exchange.ex_world_engine.service.TemporalDecayService.WeightedNews;
import de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.NewsGenerationOrchestrator;
import de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.strategy.NewsGenerationResult;
import de.pamunda.nexusfin.exchange.ex_world_engine.domain.CategoryDistributionTracker;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class NewsService {

    private final VectorDbService vectorDbService;
    private final NewsGenerationOrchestrator orchestrator;
    private final CategoryDistributionTracker categoryTracker;
    private final TemporalDecayService temporalDecayService;
    private final SchedulerService schedulerService;

    public NewsService(
            VectorDbService vectorDbService,
            NewsGenerationOrchestrator orchestrator,
            CategoryDistributionTracker categoryTracker,
            TemporalDecayService temporalDecayService,
            SchedulerService schedulerService
    ) {
        this.vectorDbService = vectorDbService;
        this.orchestrator = orchestrator;
        this.categoryTracker = categoryTracker;
        this.temporalDecayService = temporalDecayService;
        this.schedulerService = schedulerService;
    }

    @PostConstruct
    public void scheduleNewsGeneration() {
        schedulerService.registerScheduledCallback(
                CronExpression.parse("0 */2 * * * *"), this::generateAtomicNews
        );
        schedulerService.registerScheduledCallback(
                CronExpression.parse("10 */20 * * * *"), this::generateNewsCluster
        );
        schedulerService.registerScheduledCallback(
                CronExpression.parse("20 0 */2 * * *"), this::generateMarketSummary
        );
        schedulerService.registerScheduledCallback(
                CronExpression.parse("0 1 16 * * *"), this::generateDailyDigest
        );
    }

    public void generateAtomicNews(LocalDateTime timestamp) {
        // 1. Check time-of-day rate and potentially skip
        TimeOfDay timeOfDay = TimeOfDay.fromTimestamp(timestamp);
        if (shouldSkipBasedOnRate(timeOfDay)) {
            log.debug("Skipping atomic news generation at {} due to time-of-day rate ({})",
                    timestamp, timeOfDay);
            return;
        }

        // 2. Select category with distribution enforcement
        NewsCategory category = categoryTracker.selectNextCategory(timestamp);

        // 3. Assign random impact score (70% low, 25% medium, 5% high)
        float impactScore = assignRandomImpactScore();

        // 4. Create request DTO
        NewsGenerationRequest request = NewsGenerationRequest.forAtomicNews(
                category,
                timestamp,
                impactScore
        );

        // 5. Generate news via orchestrator
        List<NewsGenerationResult> results = orchestrator.generateNews(request);

        // 6. Store in Qdrant
        for (NewsGenerationResult result : results) {
            vectorDbService.addNewsItem(result.content(), result.metadata());
        }

        log.info("Generated atomic news at {} - category: {}, impact: {}", timestamp, category, impactScore);
    }

    public void generateNewsCluster(LocalDateTime timestamp) {
        // 1. Check time-of-day rate and potentially skip
        TimeOfDay timeOfDay = TimeOfDay.fromTimestamp(timestamp);
        if (shouldSkipBasedOnRate(timeOfDay)) {
            log.debug("Skipping news cluster generation at {} due to time-of-day rate ({})",
                    timestamp, timeOfDay);
            return;
        }

        // 2. Determine theme from recent news
        String theme = determineThemeFromRecentNews(timestamp);
        if (theme == null) {
            log.debug("No suitable theme found for news cluster at {}, skipping", timestamp);
            return;
        }

        // 3. Select primary category (use most frequent category from recent news)
        NewsCategory primaryCategory = categoryTracker.selectNextCategory(timestamp);

        // 4. Create request DTO
        NewsGenerationRequest request = NewsGenerationRequest.forNewsCluster(
                primaryCategory,
                timestamp,
                theme
        );

        // 5. Generate news cluster via orchestrator
        List<NewsGenerationResult> results = orchestrator.generateNews(request);

        // 6. Store all items in Qdrant
        for (NewsGenerationResult result : results) {
            vectorDbService.addNewsItem(result.content(), result.metadata());
        }

        log.info("Generated news cluster at {} - theme: {}, items: {}", timestamp, theme, results.size());
    }

    public void generateMarketSummary(LocalDateTime timestamp) {
        // 1. Check time-of-day rate and potentially skip
        TimeOfDay timeOfDay = TimeOfDay.fromTimestamp(timestamp);
        if (shouldSkipBasedOnRate(timeOfDay)) {
            log.debug("Skipping market summary generation at {} due to time-of-day rate ({})",
                    timestamp, timeOfDay);
            return;
        }

        // 2. Create request DTO
        NewsGenerationRequest request = NewsGenerationRequest.forMarketSummary(timestamp);

        // 3. Generate summary via orchestrator
        List<NewsGenerationResult> summaries = orchestrator.generateNews(request);

        // 4. Store in Qdrant
        for (NewsGenerationResult generationResult : summaries) {
            String fullText = generationResult.metadata().title(); // TODO: Extract full content from LLM response
            vectorDbService.addNewsItem(fullText, generationResult.metadata());
        }

        log.info("Generated market summary at {}", timestamp);
    }

    public void generateDailyDigest(LocalDateTime timestamp) {
        // 1. Check time-of-day rate and potentially skip
        TimeOfDay timeOfDay = TimeOfDay.fromTimestamp(timestamp);
        if (shouldSkipBasedOnRate(timeOfDay)) {
            log.debug("Skipping daily digest generation at {} due to time-of-day rate ({})",
                    timestamp, timeOfDay);
            return;
        }

        // 2. Create request DTO
        NewsGenerationRequest request = NewsGenerationRequest.forDailyDigest(timestamp);

        // 3. Generate digest via orchestrator
        List<NewsGenerationResult> digests = orchestrator.generateNews(request);

        // 4. Store in Qdrant
        for (NewsGenerationResult generationResult : digests) {
            String fullText = generationResult.metadata().title(); // TODO: Extract full content from LLM response
            vectorDbService.addNewsItem(fullText, generationResult.metadata());
        }

        log.info("Generated daily digest at {}", timestamp);
    }

    /**
     * Probabilistically skips generation based on time-of-day execution rate.
     *
     * @param timeOfDay The time-of-day tier
     * @return true if generation should be skipped, false otherwise
     */
    private boolean shouldSkipBasedOnRate(TimeOfDay timeOfDay) {
        double executionRate = timeOfDay.getExecutionRate();
        double random = ThreadLocalRandom.current().nextDouble();
        return random >= executionRate;
    }

    /**
     * Assigns random impact score based on distribution:
     * - 70% low (1-4)
     * - 25% medium (5-7)
     * - 5% high (8-10)
     *
     * @return Impact score between 1 and 10
     */
    private float assignRandomImpactScore() {
        double random = ThreadLocalRandom.current().nextDouble();

        if (random < 0.70) {
            // Low impact: 1-4
            return ThreadLocalRandom.current().nextInt(1, 5);
        } else if (random < 0.95) {
            // Medium impact: 5-7
            return ThreadLocalRandom.current().nextInt(5, 8);
        } else {
            // High impact: 8-10
            return ThreadLocalRandom.current().nextInt(8, 11);
        }
    }

    /**
     * Determines a theme from recent news for cluster generation.
     * Returns null if no suitable theme is found.
     *
     * @param timestamp Current timestamp
     * @return Theme string or null
     */
    private String determineThemeFromRecentNews(LocalDateTime timestamp) {
        // Retrieve recent news (last 4 hours)
        List<WeightedNews> recentNews = temporalDecayService.getWeightedContext(
                timestamp,
                Duration.ofHours(4),
                Duration.ofHours(8)
        );

        if (recentNews.isEmpty()) {
            return null;
        }

        // Simple implementation: Use title of highest-weighted news as theme inspiration
        // In a more sophisticated version, you could use LLM to extract common themes
        WeightedNews topNews = recentNews.get(0);
        String topTitle = topNews.metadata().title();

        // Extract a simple theme (e.g., first few words or category-based)
        return topNews.metadata().category().name().replace("_", " ").toLowerCase() +
                " developments related to " + topTitle.substring(0, Math.min(50, topTitle.length()));
    }
}
