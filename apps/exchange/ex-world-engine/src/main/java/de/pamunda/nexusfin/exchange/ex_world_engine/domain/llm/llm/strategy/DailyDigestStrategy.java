package de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsCategory;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsGenerationRequest;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsMetadata;
import de.pamunda.nexusfin.exchange.ex_world_engine.service.VectorDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prompt engineering strategy for daily digest generation.
 * Generates 500-800 word comprehensive end-of-day market recap.
 */
@Slf4j
public class DailyDigestStrategy implements PromptEngineeringStrategy {

    private final VectorDbService vectorDbService;
    private final String template;
    private final ObjectMapper objectMapper;

    public DailyDigestStrategy(
            VectorDbService vectorDbService,
            String template
    ) {
        this.vectorDbService = vectorDbService;
        this.template = template;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String buildPrompt(NewsGenerationRequest request) {
        // 1. Define trading day window (9:30 AM - 4:00 PM EST)
        LocalDateTime tradingDayStart = request.timestamp().withHour(9).withMinute(30).withSecond(0);
        LocalDateTime tradingDayEnd = request.timestamp().withHour(16).withMinute(0).withSecond(0);

        // 2. Retrieve all news from trading day
        List<Document> tradingDayNews = vectorDbService.getNewsBetween(tradingDayStart, tradingDayEnd);

        // 3. Separate atomic news from market summaries
        List<Document> atomicNews = tradingDayNews.stream()
                .filter(doc -> {
                    Object type = doc.getMetadata().get("news_type");
                    return type != null && type.toString().equals("ATOMIC_NEWS");
                })
                .toList();

        List<Document> marketSummaries = tradingDayNews.stream()
                .filter(doc -> {
                    Object type = doc.getMetadata().get("news_type");
                    return type != null && type.toString().equals("MARKET_SUMMARY");
                })
                .toList();

        // 4. Format categorized news
        String categorizedNews = formatCategorizedNews(atomicNews);
        String summariesText = formatMarketSummaries(marketSummaries);

        // 5. Fill template placeholders
        return template
                .replace("{trading_day_date}", request.timestamp().toLocalDate().toString())
                .replace("{news_count}", String.valueOf(atomicNews.size()))
                .replace("{categorized_news}", categorizedNews)
                .replace("{market_summaries}", summariesText);
    }

    @Override
    public List<NewsGenerationResult> parseResponse(String llmResponse, NewsGenerationRequest request) {
        try {
            JsonNode rootNode = objectMapper.readTree(llmResponse);

            String title = rootNode.get("title").asText();
            String content = rootNode.get("content").asText();
            float sentimentScore = (float) rootNode.get("overall_sentiment").asDouble();

            NewsMetadata metadata = new NewsMetadata(
                    title,
                    NewsCategory.SYNTHESIS, // Daily digests are SYNTHESIS category
                    sentimentScore,
                    9.0f, // Daily digests have high impact
                    request.timestamp(),
                    null, // thread_id = null in Step 1
                    NewsMetadata.Type.DAILY_DIGEST
            );

            return List.of(new NewsGenerationResult(metadata, title + "\n\n" + content));

        } catch (Exception e) {
            log.error("Failed to parse daily digest LLM response: {}", llmResponse, e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    /**
     * Formats atomic news by category for digest.
     */
    private String formatCategorizedNews(List<Document> news) {
        if (news.isEmpty()) {
            return "No atomic news during trading hours.";
        }

        StringBuilder formatted = new StringBuilder();

        // Group by category
        news.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        doc -> doc.getMetadata().get("category")
                ))
                .forEach((category, items) -> {
                    formatted.append(String.format("\n%s (%d items):\n", category, items.size()));
                    items.stream().limit(5).forEach(doc ->
                            formatted.append(String.format("  - %s\n",
                                    doc.getMetadata().get("title")))
                    );
                });

        return formatted.toString();
    }

    /**
     * Formats market summaries from the day.
     */
    private String formatMarketSummaries(List<Document> summaries) {
        if (summaries.isEmpty()) {
            return "No market summaries available.";
        }

        StringBuilder formatted = new StringBuilder();
        for (Document summary : summaries) {
            String title = (String) summary.getMetadata().get("title");
            String content = summary.getText().substring(0, Math.min(200, summary.getText().length()));
            formatted.append(String.format("\n- %s\n  %s...\n", title, content));
        }

        return formatted.toString();
    }
}
