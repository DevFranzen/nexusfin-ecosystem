package de.pamunda.nexusfin.exchange.ex_world_engine.domain.llm.llm.strategy;

import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsGenerationRequest;

import java.util.List;

/**
 * Strategy interface for prompt engineering in news generation.
 * Each implementation handles a specific type of news generation (ATOMIC, CLUSTER, SUMMARY, DIGEST).
 *
 * Responsibilities:
 * 1. Load and fill prompt templates with placeholders
 * 2. Retrieve relevant context from Qdrant via TemporalDecayService
 * 3. Format context using ContextFormatter
 * 4. Parse LLM JSON responses into NewsGenerationResult objects (metadata + content)
 */
public interface PromptEngineeringStrategy {

    /**
     * Builds the complete prompt for the LLM by:
     * 1. Loading the template from resources
     * 2. Retrieving weighted context from Qdrant
     * 3. Filling template placeholders
     *
     * @param request The news generation request with parameters
     * @return The complete prompt ready for LLM
     */
    String buildPrompt(NewsGenerationRequest request);

    /**
     * Parses the LLM response JSON into NewsGenerationResult object(s).
     * For ATOMIC/SUMMARY/DIGEST: returns single-item list
     * For CLUSTER: returns list of 3-5 items
     *
     * @param llmResponse The JSON response from the LLM
     * @param request The original request (for context like timestamp, category)
     * @return List of NewsGenerationResult objects (metadata + content) parsed from response
     */
    List<NewsGenerationResult> parseResponse(String llmResponse, NewsGenerationRequest request);
}
