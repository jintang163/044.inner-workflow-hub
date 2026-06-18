package com.innerworkflow.ai.task;

import com.innerworkflow.common.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiWeeklyTrainingTask {

    private final AiRecommendationService aiRecommendationService;

    @Scheduled(cron = "0 0 2 * * MON")
    public void triggerWeeklyTraining() {
        log.info("开始执行周度AI模型训练定时任务...");
        try {
            aiRecommendationService.triggerWeeklyTraining();
            log.info("周度AI模型训练定时任务执行完成");
        } catch (Exception e) {
            log.error("周度AI模型训练定时任务执行失败: {}", e.getMessage(), e);
        }
    }
}
