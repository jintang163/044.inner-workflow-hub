package com.innerworkflow.form.task;

import com.innerworkflow.form.service.WfFormDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormDraftCleanTask {

    private final WfFormDraftService formDraftService;

    @Scheduled(cron = "0 30 2 * * ?")
    public void cleanExpiredDrafts() {
        log.info("定时清理过期草稿任务开始执行");
        try {
            int count = formDraftService.cleanExpiredDrafts(30);
            log.info("定时清理过期草稿任务完成，清理数量: {}", count);
        } catch (Exception e) {
            log.error("定时清理过期草稿任务执行失败: {}", e.getMessage(), e);
        }
    }
}
