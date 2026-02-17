package de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.pamunda.nexusfin.exchange.ex_world_engine.domain.TimeOfDay;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsGenerationRequest;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsMetadata;
import de.pamunda.nexusfin.exchange.ex_world_engine.service.TemporalDecayService;
import de.pamunda.nexusfin.exchange.ex_world_engine.service.TemporalDecayService.WeightedNews;
import de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.ContextFormatter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

/**
 * Prompt engineering strategy for atomic news generation.
 * Generates single news items in a specific category with weighted historical context.
 */
@Slf4j
public class  AtomicNewsStrategy implements PromptEngineeringStrategy {

    private final TemporalDecayService temporalDecayService;
    private final ContextFormatter contextFormatter;
    private final String template;
    private final ObjectMapper objectMapper;

    public AtomicNewsStrategy(
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
        // 1. Retrieve weighted context from Qdrant (last 2 hours primary, last 24 hours background)
        List<WeightedNews> context = temporalDecayService.getWeightedContext(
                request.timestamp(),
                Duration.ofHours(2),
                Duration.ofHours(24)
        );

        // 2. Format context using ContextFormatter
        String contextSummary = context.isEmpty()
                ? "No recent news available."
                : contextFormatter.buildWeightedContextSummary(context, 20);

        // 3. Determine time-of-day tier
        TimeOfDay timeOfDay = TimeOfDay.fromTimestamp(request.timestamp());

        // 4. Fill template placeholders
        return template
                .replace("{category}", request.category().name())
                .replace("{impact_score}", String.valueOf((int) request.impactScore()))
                .replace("{timestamp}", request.timestamp().toString())
                .replace("{time_of_day}", timeOfDay.name())
                .replace("{weighted_news_summary}", contextSummary);
    }

    @Override
    public List<NewsGenerationResult> parseResponse(String llmResponse, NewsGenerationRequest request) {
        try {
            JsonNode rootNode = objectMapper.readTree(llmResponse);

            String title = rootNode.get("title").asText();
            String content = rootNode.get("content").asText();
            String sentiment = rootNode.get("sentiment").asText();
            int sentimentIntensity = rootNode.get("sentiment_intensity").asInt();

            // Map sentiment to score: BULLISH = positive, BEARISH = negative, NEUTRAL = 0
            float sentimentScore = switch (sentiment) {
                case "BULLISH" -> sentimentIntensity / 5.0f;
                case "BEARISH" -> -sentimentIntensity / 5.0f;
                default -> 0.0f;
            };

            NewsMetadata metadata = new NewsMetadata(
                    title,
                    request.category(),
                    sentimentScore,
                    request.impactScore(),
                    request.timestamp(),
                    null, // thread_id = null in Step 1
                    NewsMetadata.Type.ATOMIC_NEWS
            );

            return List.of(new NewsGenerationResult(metadata, title + "\n\n" + content));

        } catch (Exception e) {
            log.error("Failed to parse atomic news LLM response: {}", llmResponse, e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }
}
