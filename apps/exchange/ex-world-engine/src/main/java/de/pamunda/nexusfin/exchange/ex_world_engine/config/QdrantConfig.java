package de.pamunda.nexusfin.exchange.ex_world_engine.config;

import io.qdrant.client.QdrantClient;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.concurrent.ExecutionException;

@Configuration
@DependsOn("vectorStore")
@ConditionalOnProperty(
        prefix = "spring.ai.vectorstore.qdrant",
        name = "validate-schema-dimension",
        havingValue = "true",
        matchIfMissing = false
)
public class QdrantConfig {

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;
    private final QdrantClient qdrantClient;
    private final EmbeddingModel embeddingModel;

    public QdrantConfig(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        this.qdrantClient = qdrantClient;
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    public void init() throws ExecutionException, InterruptedException {
        if(qdrantClient.collectionExistsAsync(collectionName).get()) {
            long embeddingModelDimensions = embeddingModel.dimensions();
            long vectorDBDimension = this.qdrantClient
                    .getCollectionInfoAsync(collectionName).get()
                    .getConfig()
                    .getParams()
                    .getVectorsConfig()
                    .getParams()
                    .getSize();

            if(embeddingModelDimensions != vectorDBDimension) {
                throw new RuntimeException("Vectordimensions of configured embedding model does not match the dimension of collections vector in Qdrant-DB");
            }
        }
    }
}
