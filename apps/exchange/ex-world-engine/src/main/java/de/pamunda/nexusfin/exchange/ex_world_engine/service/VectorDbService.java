package de.pamunda.nexusfin.exchange.ex_world_engine.service;

import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsMetadata;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.ThreadMetadata;
import io.qdrant.client.QdrantClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class VectorDbService {

    public enum StoreType{
        NEWS,
        THREAD
    }

    private final VectorStore newsVectorStore;
    private final VectorStore threadsVectorStore;
    private final QdrantClient qdrantClient;

    @Value("${spring.ai.vectorstore.qdrant.collections.news}")
    private String newsCollectionName;

    public VectorDbService (@Qualifier("newsVectorStore") VectorStore newsVectorStore,
                            @Qualifier("threadsVectorStore") VectorStore threadsVectorStore,
                            QdrantClient qdrantClient)
    {
        this.newsVectorStore = newsVectorStore;
        this.threadsVectorStore = threadsVectorStore;
        this.qdrantClient = qdrantClient;
    }

    public void addNewsItem(String content, NewsMetadata meta){
        this.addItem(content, meta.asMap(), StoreType.NEWS);
    }

    public void addThreadItem(String content, ThreadMetadata meta){
        this.addItem(content, meta.asMap(), StoreType.THREAD);
    }

    private void addItem(String content, Map<String, Object> meta, StoreType type){
        List<Document> documents = new ArrayList<>();
        documents.add( Document.builder()
                .metadata(meta)
                .text(content)
                .build()
        );
        switch (type){
            case NEWS -> this.newsVectorStore.add(documents);
            case THREAD -> this.threadsVectorStore.add(documents);
            default -> throw new RuntimeException("Unkown store Type!");
        }
    }

    public LocalDateTime getLastNewsTimestamp() {
        LocalDateTime defaultTimestamp = LocalDateTime.now().minusDays(2);

        // Check if collection has any documents
        long count = getNewsCount();
        if (count == 0) {
            return defaultTimestamp;
        }

        // Query for most recent document
        List<Document> recentDocs = newsVectorStore.similaritySearch(
                SearchRequest.builder().query("").topK(1).build()
        );

        if (recentDocs.isEmpty()) {
            return defaultTimestamp;
        }

        try {
            String timestampStr = (String) recentDocs.get(0).getMetadata().get("publication_timestamp");
            return LocalDateTime.parse(timestampStr);
        } catch (Exception e) {
            return defaultTimestamp;
        }
    }

    /**
     * Retrieves all news items published since the given timestamp.
     *
     * @param since Timestamp to retrieve news from
     * @return List of news documents
     */
    public List<Document> getRecentNews(LocalDateTime since) {
        var filterExpression = new FilterExpressionBuilder()
                .gte("publication_timestamp", since.toString())
                .build();

        return newsVectorStore.similaritySearch(
                SearchRequest.builder().query("")
                        .topK(1000)
                        .filterExpression(filterExpression)
                        .build()
        );
    }

    /**
     * Retrieves all news items published between start and end timestamps.
     *
     * @param start Start timestamp (inclusive)
     * @param end End timestamp (exclusive)
     * @return List of news documents
     */
    public List<Document> getNewsBetween(LocalDateTime start, LocalDateTime end) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        var filterExpression = b.and(
                        b.gte("publication_timestamp", start.toString()),
                        b.lt("publication_timestamp", end.toString())
                )
                .build();

        return newsVectorStore.similaritySearch(
                SearchRequest.builder().query("")
                        .topK(1000)
                        .filterExpression(filterExpression)
                        .build()
        );
    }

    /**
     * Returns the total count of news items in the database using Qdrant collection info.
     *
     * @return Number of news documents
     */
    public long getNewsCount() {
        try {
            var collectionInfo = qdrantClient.getCollectionInfoAsync(newsCollectionName).get();
            return collectionInfo.getPointsCount();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return 0L;
        }
    }

}
