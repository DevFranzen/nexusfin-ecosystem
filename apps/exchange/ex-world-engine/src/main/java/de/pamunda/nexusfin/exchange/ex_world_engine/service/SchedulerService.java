package de.pamunda.nexusfin.exchange.ex_world_engine.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class SchedulerService {

    @Value("${app.catch-up-generation}")
    private Boolean catchUpGeneration;
    private final Map<CronExpression, List<Consumer<LocalDateTime>>> callbacks;
    private final TaskScheduler taskScheduler;
    private final VectorDbService vectorDbService;

    public SchedulerService(TaskScheduler taskScheduler, VectorDbService vectorDbService){
        this.callbacks = new HashMap<>();
        this.taskScheduler = taskScheduler;
        this.vectorDbService = vectorDbService;
    }

    public void registerScheduledCallback(CronExpression cron, Consumer<LocalDateTime> callback){
        if( !this.callbacks.containsKey(cron)){
            this.callbacks.put(cron, new ArrayList<>());
        }
        var callbackList = this.callbacks.get(cron);
        if( callbackList == null) {
            callbackList = new ArrayList<>();
            callbacks.put(cron, callbackList);
        }
        callbackList.add(callback);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startGenerating() {
        log.info("Start generating News");
        if(this.catchUpGeneration){
            var virtualDateTime = this.vectorDbService.getLastNewsTimestamp().withNano(0);
            while( virtualDateTime.isBefore(LocalDateTime.now()) ) {
                this.runVirtualTaskScheduler(virtualDateTime);
                virtualDateTime = virtualDateTime.plusSeconds(1);
            }
        }
        this.startRealtimeScheduling();
    }

    private void runVirtualTaskScheduler(LocalDateTime virtualDateTime) {
        this.callbacks.entrySet().stream()
                .filter(entry ->
                    virtualDateTime.equals(
                            entry.getKey().next(virtualDateTime.minusSeconds(1))
                    )
                ).forEach(entry ->
                        entry.getValue()
                                .forEach(callback -> callback.accept(virtualDateTime))
                );
    }

    private void startRealtimeScheduling() {
        this.callbacks.forEach((key, callbackList) -> {
            var trigger = new CronTrigger(key.toString());
            for (var callback : callbackList) {
                taskScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        callback.accept(LocalDateTime.now());
                    }
                }, trigger);
            }
        });
    }

}
