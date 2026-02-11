package de.pamunda.nexusfin.exchange.ex_world_engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public record VectorDBMetadata(

        @JsonProperty("title")
        String title,

        @JsonProperty("category")
        Category category,

        @JsonProperty("tags")
        List<String> tags,

        @JsonProperty("schema_version")
        String schemaVersion,

        @JsonProperty("tenant_id")
        Integer tenantId
){
    public enum Category {
        FINANCE,
        SPORTS,
        ECONOMY
    }

    private final static ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> asMap() {
        return mapper.convertValue(this, new TypeReference<Map<String, Object>>() {});
    }

}
