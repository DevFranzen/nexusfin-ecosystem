package de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm;

import de.pamunda.nexusfin.exchange.ex_world_engine.domain.GenerationType;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsGenerationRequest;
import de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.strategy.NewsGenerationResult;
import de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.strategy.PromptEngineeringStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Orchestrator for news generation using strategy pattern.
 * Delegates to appropriate PromptEngineeringStrategy based on GenerationType.
 *
 * Flow:
 * 1. Receives NewsGenerationRequest
 * 2. Selects appropriate strategy (ATOMIC, CLUSTER, SUMMARY, DIGEST)
 * 3. Strategy builds prompt from template + Qdrant context
 * 4. Calls ChatClient with built prompt
 * 5. Strategy parses LLM response into NewsGenerationResult (metadata + content)
 */
@Slf4j
@Component
public class NewsGenerationOrchestrator {

    private final Map<GenerationType, PromptEngineeringStrategy> strategies;
    private final ChatClient chatClient;

    public NewsGenerationOrchestrator(
            Map<GenerationType, PromptEngineeringStrategy> strategies,
            ChatClient chatClient
    ) {
        this.strategies = strategies;
        this.chatClient = chatClient;
    }

    /**
     * Generates news using the appropriate strategy.
     *
     * @param request The news generation request
     * @return List of NewsGenerationResult (metadata + content) for generated news
     */
    public List<NewsGenerationResult> generateNews(NewsGenerationRequest request) {
        // 1. Select appropriate strategy based on generation type
        PromptEngineeringStrategy strategy = strategies.get(request.type());
        if (strategy == null) {
            log.error("No strategy found for generation type: {}", request.type());
            throw new IllegalStateException("No strategy configured for type: " + request.type());
        }

        // 2. Build prompt using strategy
        String prompt = strategy.buildPrompt(request);

        log.debug("Built prompt for {} generation (length: {} chars)", request.type(), prompt.length());

        // 3. Call ChatClient with built prompt
        String llmResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.debug("Received LLM response for {} generation (length: {} chars)", request.type(), llmResponse.length());

        // 4. Parse response using strategy
        List<NewsGenerationResult> results = strategy.parseResponse(llmResponse, request);

        log.info("Generated {} news item(s) of type {}", results.size(), request.type());

        return results;
    }
}
