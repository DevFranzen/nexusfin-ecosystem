package de.pamunda.nexusfin.exchange.ex_world_engine.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class NewsService {

    private final VectorDbService vectorDbService;
    private final ChatClient newsChatClient;
    private final SchedulerService schedulerService;

    public NewsService(VectorDbService vectorDbService, @Qualifier("newsChatClient") ChatClient chatClient, SchedulerService schedulerService){
        this.vectorDbService = vectorDbService;
        this.newsChatClient = chatClient;
        this.schedulerService = schedulerService;
    }

    @PostConstruct
    public void scheduleNewsGeneration() {
        schedulerService.registerScheduledCallback(
                CronExpression.parse("0 */2 * * * *"), this::generateAtomicNews
        );
        schedulerService.registerScheduledCallback(
                CronExpression.parse("10 */20 * * * *"), this::generateNewsCluster
        );
        schedulerService.registerScheduledCallback(
                CronExpression.parse("20 0 */2 * * *"), this::generateMarketSummary
        );
        schedulerService.registerScheduledCallback(
                CronExpression.parse("0 1 16 * * *"), this::generateDailyDigest
        );
    }

    public void generateAtomicNews(LocalDateTime timestamp) {
        log.info("Generating atomic news at {}", timestamp.toString());
    }

    public void generateNewsCluster(LocalDateTime timestamp) {
        log.info("Generating news cluster at {}", timestamp.toString());
    }

    public void generateMarketSummary(LocalDateTime timestamp) {
        log.info("Generating market summary at {}", timestamp.toString());
    }

    public void generateDailyDigest(LocalDateTime timestamp) {
        log.info("Generating daily digest at {}", timestamp.toString());
    }


}
