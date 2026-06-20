package com.innerworkflow.approval.task;

import com.innerworkflow.approval.service.WfEscalationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class WfEscalationQuartzJob implements Job {

    @Autowired
    private WfEscalationService escalationService;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Quartz定时任务开始执行: 超时升级扫描");
        try {
            escalationService.processEscalation();
            log.info("Quartz定时任务执行完成: 超时升级扫描");
        } catch (Exception e) {
            log.error("Quartz定时任务执行异常: 超时升级扫描, error={}", e.getMessage(), e);
        }
    }
}
