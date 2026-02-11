package de.pamunda.nexusfin.exchange.ex_world_engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.ai.document.MetadataMode;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(
        prefix = "spring.ai.dedicated-embedding",
        name = "enabled",
        havingValue = "true"
)
public class EmbeddingConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.ai.dedicated-embedding")
    public EmbeddingProperties embeddingProps() {
        return new EmbeddingProperties();
    }

    @Primary
    @Bean(name = "customEmbeddingModel")
    public EmbeddingModel customEmbeddingModel(EmbeddingProperties props) {
        return switch (props.getProvider().toLowerCase()) {
            case "openai" -> openAiEmbeddingModel(props);
            case "ollama" -> ollamaEmbeddingModel(props);
            // add customized providers if needed
            default -> throw new IllegalArgumentException("Unknown embeddings provider: " + props.getProvider());
        };
    }

    private EmbeddingModel ollamaEmbeddingModel(EmbeddingProperties props) {
        var options = OllamaEmbeddingOptions.builder().model(props.getModel())
                .build();

        return OllamaEmbeddingModel.builder()
                .ollamaApi(OllamaApi.builder().baseUrl(props.getBase_url()).build())
                .defaultOptions(options)
                .build();
    }

    private EmbeddingModel openAiEmbeddingModel(EmbeddingProperties props) {
        var options = OpenAiEmbeddingOptions.builder().model(props.getModel())
                .build();
        return new OpenAiEmbeddingModel(
                OpenAiApi.builder().apiKey(props.getApi_key()).baseUrl(props.getBase_url()).build(),
                MetadataMode.NONE,
                options
        );
    }

    @Setter
    @Getter
    public static class EmbeddingProperties {
        private String provider;
        private String api_key;
        private String base_url;
        private String model;
    }
}
