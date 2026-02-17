package de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsGenerationRequest;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsMetadata;
import de.pamunda.nexusfin.exchange.ex_world_engine.service.TemporalDecayService;
import de.pamunda.nexusfin.exchange.ex_world_engine.service.TemporalDecayService.WeightedNews;
import de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.ContextFormatter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Prompt engineering strategy for news cluster generation.
 * Generates 3-5 related news items forming a thematic narrative cluster.
 */
@Slf4j
public class NewsClusterStrategy implements PromptEngineeringStrategy {

    private final TemporalDecayService temporalDecayService;
    private final ContextFormatter contextFormatter;
    private final String template;
    private final ObjectMapper objectMapper;

    public NewsClusterStrategy(
            TemporalDecayService temporalDecayService,
            ContextFormatter contextFormatter,
            String template
    ) {
        this.temporalDecayService = temporalDecayService;
        this.contextFormatter = contextFormatter;
        this.template = template;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String buildPrompt(NewsGenerationRequest request) {
        // 1. Retrieve weighted context from Qdrant (last 2 hours)
        List<WeightedNews> context = temporalDecayService.getWeightedContext(
                request.timestamp(),
                Duration.ofHours(2),
                Duration.ofHours(6)
        );

        // 2. Format context using ContextFormatter
        String contextSummary = context.isEmpty()
                ? "No recent news available."
                : contextFormatter.buildWeightedContextSummary(context, 15);

        // 3. Fill template placeholders
        return template
                .replace("{theme}", request.theme())
                .replace("{primary_category}", request.category().name())
                .replace("{weighted_news_summary}", contextSummary);
    }

    @Override
    public List<NewsGenerationResult> parseResponse(String llmResponse, NewsGenerationRequest request) {
        try {
            JsonNode rootNode = objectMapper.readTree(llmResponse);

            if (!rootNode.isArray()) {
                throw new IllegalArgumentException("Expected JSON array for news cluster");
            }

            List<NewsGenerationResult> newsItems = new ArrayList<>();

            for (JsonNode itemNode : rootNode) {
                String title = itemNode.get("title").asText();
                String content = itemNode.get("content").asText();
                String sentiment = itemNode.get("sentiment").asText();
                int sentimentIntensity = itemNode.get("sentiment_intensity").asInt();
                float impactScore = (float) itemNode.get("impact_score").asDouble();

                // Map sentiment to score
                float sentimentScore = switch (sentiment) {
                    case "BULLISH" -> sentimentIntensity / 5.0f;
                    case "BEARISH" -> -sentimentIntensity / 5.0f;
                    default -> 0.0f;
                };

                NewsMetadata metadata = new NewsMetadata(
                        title,
                        request.category(), // All items in cluster use the primary category
                        sentimentScore,
                        impactScore,
                        request.timestamp().plusMinutes(newsItems.size() * 2), // Spread over 10 minutes
                        null, // thread_id = null in Step 1
                        NewsMetadata.Type.ATOMIC_NEWS // Cluster items are still atomic news
                );

                newsItems.add(new NewsGenerationResult(metadata, title + "\n\n" + content));
            }

            if (newsItems.size() < 3 || newsItems.size() > 5) {
                log.warn("News cluster has {} items (expected 3-5)", newsItems.size());
            }

            return newsItems;

        } catch (Exception e) {
            log.error("Failed to parse news cluster LLM response: {}", llmResponse, e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }
}
