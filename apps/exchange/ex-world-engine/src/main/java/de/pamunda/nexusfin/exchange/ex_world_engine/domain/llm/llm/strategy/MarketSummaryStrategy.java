package de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsCategory;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsGenerationRequest;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsMetadata;
import de.pamunda.nexusfin.exchange.ex_world_engine.service.TemporalDecayService;
import de.pamunda.nexusfin.exchange.ex_world_engine.service.TemporalDecayService.WeightedNews;
import de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.ContextFormatter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

/**
 * Prompt engineering strategy for market summary generation.
 * Generates 200-300 word summary of last 2 hours of market news.
 */
@Slf4j
public class MarketSummaryStrategy implements PromptEngineeringStrategy {

    private final TemporalDecayService temporalDecayService;
    private final ContextFormatter contextFormatter;
    private final String template;
    private final ObjectMapper objectMapper;

    public MarketSummaryStrategy(
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
        // 1. Retrieve weighted context from Qdrant
        // Primary window: last 2 hours (immediate)
        // Secondary window: 2-6 hours (recent background)
        List<WeightedNews> context = temporalDecayService.getWeightedContext(
                request.timestamp(),
                Duration.ofHours(2),
                Duration.ofHours(6)
        );

        // 2. Format context - use detailed summary with top 20 items
        String contextSummary = context.isEmpty()
                ? "No recent news available for summary."
                : contextFormatter.buildSummariesText(context.stream().limit(20).toList());

        // 3. Fill template placeholders
        return template
                .replace("{timestamp}", request.timestamp().toString())
                .replace("{weighted_news_items}", contextSummary);
    }

    @Override
    public List<NewsGenerationResult> parseResponse(String llmResponse, NewsGenerationRequest request) {
        try {
            JsonNode rootNode = objectMapper.readTree(llmResponse);

            String title = rootNode.get("title").asText();
            String content = rootNode.get("content").asText();
            float sentimentScore = (float) rootNode.get("sentiment_score").asDouble();

            NewsMetadata metadata = new NewsMetadata(
                    title,
                    NewsCategory.SYNTHESIS, // Market summaries are SYNTHESIS category
                    sentimentScore,
                    7.0f, // Market summaries have medium-high impact
                    request.timestamp(),
                    null, // thread_id = null in Step 1
                    NewsMetadata.Type.MARKET_SUMMARY
            );

            return List.of(new NewsGenerationResult(metadata, title + "\n\n" + content));

        } catch (Exception e) {
            log.error("Failed to parse market summary LLM response: {}", llmResponse, e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }
}
