package de.pamunda.nexusfin.exchange.ex_world_engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record NewsMetadata(

        @JsonProperty("title")
        String title,

        @JsonProperty("category")
        NewsCategory category,

        @JsonProperty("sentiment_score")
        float sentiment_score,

        @JsonProperty("impact_score")
        float impact_score,

        @JsonProperty("publication_timestamp")
        LocalDateTime publication_timestamp,

        @JsonProperty("thread_id")
        UUID thread_id
){

    public enum Type {
        ATOMIC_NEWS,
        NEWS_CLUSTER,
        MARKET_SUMMARY,
        DAILY_DIGEST
    }

    private final static ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> asMap() {
        return mapper.convertValue(this, new TypeReference<Map<String, Object>>() {});
    }

}
