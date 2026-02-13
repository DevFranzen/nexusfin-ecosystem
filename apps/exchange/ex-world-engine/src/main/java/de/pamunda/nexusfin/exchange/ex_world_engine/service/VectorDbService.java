package de.pamunda.nexusfin.exchange.ex_world_engine.service;

import de.pamunda.nexusfin.exchange.ex_world_engine.dto.NewsMetadata;
import de.pamunda.nexusfin.exchange.ex_world_engine.dto.ThreadMetadata;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VectorDbService {

    public enum StoreType{
        NEWS,
        THREAD
    }

    private final VectorStore newsVectorStore;
    private final VectorStore threadsVectorStore;

    public VectorDbService (@Qualifier("newsVectorStore") VectorStore newsVectorStore,
                            @Qualifier("threadsVectorStore") VectorStore threadsVectorStore)
    {
        this.newsVectorStore = newsVectorStore;
        this.threadsVectorStore = threadsVectorStore;
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
        return LocalDateTime.now().minusDays(2);
    }

}
