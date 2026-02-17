package de.pamunda.nexusfin.exchange.ex_world_engine.config;

import de.pamunda.nexusfin.exchange.ex_world_engine.domain.GenerationType;
import de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.strategy.*;
import de.pamunda.nexusfin.exchange.ex_world_engine.service.TemporalDecayService;
import de.pamunda.nexusfin.exchange.ex_world_engine.service.VectorDbService;
import de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.ContextFormatter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient.Builder chatclientBuilder(OpenAiChatModel openAiChatModel){
        return ChatClient.builder(openAiChatModel);
    }

    @Bean("newsChatClient")
    public ChatClient chatClientGPT(ChatClient.Builder builder) {
        ChatOptions chatOptions = ChatOptions.builder()
                .model("gpt-4o")
                .temperature(0.8) // Higher temperature for creative news generation
                .build();

        return builder
                .defaultOptions(chatOptions)
                .defaultAdvisors(List.of(new SimpleLoggerAdvisor()))
                .build();
    }

    // Strategy beans

    @Bean
    public AtomicNewsStrategy atomicNewsStrategy(
            TemporalDecayService temporalDecayService,
            ContextFormatter contextFormatter,
            ResourceLoader resourceLoader
    ) throws IOException {
        String template = loadTemplate(resourceLoader, "prompts/atomic-news.txt");
        return new AtomicNewsStrategy(temporalDecayService, contextFormatter, template);
    }

    @Bean
    public NewsClusterStrategy newsClusterStrategy(
            TemporalDecayService temporalDecayService,
            ContextFormatter contextFormatter,
            ResourceLoader resourceLoader
    ) throws IOException {
        String template = loadTemplate(resourceLoader, "prompts/news-cluster.txt");
        return new NewsClusterStrategy(temporalDecayService, contextFormatter, template);
    }

    @Bean
    public MarketSummaryStrategy marketSummaryStrategy(
            TemporalDecayService temporalDecayService,
            ContextFormatter contextFormatter,
            ResourceLoader resourceLoader
    ) throws IOException {
        String template = loadTemplate(resourceLoader, "prompts/market-summary.txt");
        return new MarketSummaryStrategy(temporalDecayService, contextFormatter, template);
    }

    @Bean
    public DailyDigestStrategy dailyDigestStrategy(
            VectorDbService vectorDbService,
            ResourceLoader resourceLoader
    ) throws IOException {
        String template = loadTemplate(resourceLoader, "prompts/daily-digest.txt");
        return new DailyDigestStrategy(vectorDbService, template);
    }

    @Bean
    public Map<GenerationType, PromptEngineeringStrategy> promptStrategies(
            AtomicNewsStrategy atomic,
            NewsClusterStrategy cluster,
            MarketSummaryStrategy summary,
            DailyDigestStrategy digest
    ) {
        return Map.of(
                GenerationType.ATOMIC, atomic,
                GenerationType.CLUSTER, cluster,
                GenerationType.SUMMARY, summary,
                GenerationType.DIGEST, digest
        );
    }

    private String loadTemplate(ResourceLoader loader, String path) throws IOException {
        Resource resource = loader.getResource("classpath:" + path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
