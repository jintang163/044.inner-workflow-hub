package com.innerworkflow.approval.task;

import com.innerworkflow.approval.service.WfDelegationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WfDelegationTask {

    private final WfDelegationService delegationService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void processDelegationStatus() {
        log.info("开始处理委托状态更新...");
        try {
            delegationService.processDelegationStatus();
            log.info("委托状态更新处理完成");
        } catch (Exception e) {
            log.error("委托状态更新处理异常: {}", e.getMessage(), e);
        }
    }
}
