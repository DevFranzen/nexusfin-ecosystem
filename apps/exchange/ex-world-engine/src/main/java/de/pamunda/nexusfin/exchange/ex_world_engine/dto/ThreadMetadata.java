package de.pamunda.nexusfin.exchange.ex_world_engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

public record ThreadMetadata(

        @JsonProperty("thread_id")
        UUID thread_id,

        @JsonProperty("title")
        String title,

        @JsonProperty("category")
        NewsCategory category
){
    private final static ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> asMap() {
        return mapper.convertValue(this, new TypeReference<Map<String, Object>>() {});
    }

}
