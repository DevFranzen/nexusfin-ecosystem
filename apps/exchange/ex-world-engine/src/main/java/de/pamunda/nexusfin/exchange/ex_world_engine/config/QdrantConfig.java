package de.pamunda.nexusfin.exchange.ex_world_engine.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.concurrent.ExecutionException;

@Configuration
@DependsOn("vectorStore")
public class QdrantConfig {

    @Value("${spring.ai.vectorstore.qdrant.collections.news}")
    private String newsCollectionName;
    @Value("${spring.ai.vectorstore.qdrant.collections.story_threads}")
    private String threadsCollectionName;
    private final QdrantClient qdrantClient;
    private final EmbeddingModel embeddingModel;

    public QdrantConfig(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        this.qdrantClient = qdrantClient;
        this.embeddingModel = embeddingModel;
    }

    @Bean("newsVectorStore")
    public VectorStore newsVectorStore() throws ExecutionException, InterruptedException {
        this.initCollection(newsCollectionName);
        return QdrantVectorStore.builder(this.qdrantClient, this.embeddingModel)
                .collectionName(this.newsCollectionName)
                .build();
    }

    @Bean("threadsVectorStore")
    public VectorStore threadsVectorStore() throws ExecutionException, InterruptedException {
        this.initCollection(newsCollectionName);
        return QdrantVectorStore.builder(this.qdrantClient, this.embeddingModel)
                .collectionName(this.threadsCollectionName)
                .build();
    }

    @PostConstruct
    public void init() throws ExecutionException, InterruptedException {
        this.initCollection(newsCollectionName);
        this.initCollection(threadsCollectionName);
    }

    private void initCollection(String collectionName) throws ExecutionException, InterruptedException {
        if(this.collectionExists(collectionName)){
            this.checkCollectionDimensions(collectionName);
        }
        else {
            this.addCollection(collectionName);
        }
    }

    private boolean collectionExists (String collectionName) throws ExecutionException, InterruptedException {
        return qdrantClient.collectionExistsAsync(collectionName).get();
    }

    private void checkCollectionDimensions(String collectionName) throws ExecutionException, InterruptedException {
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

    private void addCollection(String collectionName) throws ExecutionException, InterruptedException {
        long embeddingModelDimensions = embeddingModel.dimensions();
        qdrantClient.createCollectionAsync(collectionName,
                        Collections.VectorParams.newBuilder()
                                .setDistance(Collections.Distance.Cosine)
                                .setSize(embeddingModelDimensions)
                                .build()
                )
                .get();
    }
}
