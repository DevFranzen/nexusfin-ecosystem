package de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm;

import de.pamunda.nexusfin.exchange.ex_world_engine.service.TemporalDecayService.WeightedNews;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ContextFormatter {

    /**
     * Builds a simple context summary with titles and weights.
     */
    public String buildContextSummary(List<WeightedNews> contextNews, int maxItems) {
        if (contextNews == null || contextNews.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        int count = Math.min(maxItems, contextNews.size());

        for (int i = 0; i < count; i++) {
            WeightedNews wn = contextNews.get(i);
            summary.append(String.format("- %s (weight: %.2f)\n",
                    wn.metadata().title(),
                    wn.weight()));
        }

        return summary.toString();
    }

    /**
     * Builds weighted context summary with category info.
     */
    public String buildWeightedContextSummary(List<WeightedNews> contextNews, int maxItems) {
        if (contextNews == null || contextNews.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        int count = Math.min(maxItems, contextNews.size());

        for (int i = 0; i < count; i++) {
            WeightedNews wn = contextNews.get(i);
            summary.append(String.format("Weight: %.2f | %s | %s\n",
                    wn.weight(),
                    wn.metadata().category().name(),
                    wn.metadata().title()));
        }

        return summary.toString();
    }

    /**
     * Builds categorized summary grouping news by category.
     */
    public String buildCategorizedSummary(List<WeightedNews> news) {
        if (news == null || news.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        news.stream()
                .collect(Collectors.groupingBy(wn -> wn.metadata().category()))
                .forEach((category, items) -> {
                    summary.append(String.format("%s (%d items):\n", category.name(), items.size()));
                    items.stream().limit(3).forEach(wn ->
                            summary.append(String.format("  - %s\n", wn.metadata().title()))
                    );
                });

        return summary.toString();
    }

    /**
     * Builds text from summaries with content preview.
     */
    public String buildSummariesText(List<WeightedNews> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        for (WeightedNews wn : summaries) {
            String preview = wn.content().substring(0, Math.min(200, wn.content().length()));
            text.append(String.format("- %s\n", preview));
        }

        return text.toString();
    }
}
