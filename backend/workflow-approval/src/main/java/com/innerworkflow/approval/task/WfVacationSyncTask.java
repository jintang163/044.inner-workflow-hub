package com.innerworkflow.approval.task;

import com.innerworkflow.approval.service.WfApprovalService;
import com.innerworkflow.approval.service.WfUserVacationService;
import com.innerworkflow.approval.vacation.VacationCalendarAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WfVacationSyncTask {

    private final WfUserVacationService userVacationService;
    private final WfApprovalService approvalService;
    private final Map<String, VacationCalendarAdapter> vacationAdapters;

    @Scheduled(cron = "0 0 2 * * ?")
    public void syncAllVacations() {
        log.info("开始全量休假数据同步...");

        for (Map.Entry<String, VacationCalendarAdapter> entry : vacationAdapters.entrySet()) {
            VacationCalendarAdapter adapter = entry.getValue();
            try {
                if (adapter.isEnabled()) {
                    log.info("开始从{}同步休假数据", adapter.getSourceName());
                    userVacationService.syncAllVacations(adapter.getSourceType());
                    log.info("从{}同步休假数据完成", adapter.getSourceName());
                }
            } catch (Exception e) {
                log.error("从{}同步休假数据失败, error={}", adapter.getSourceName(), e.getMessage(), e);
            }
        }

        log.info("全量休假数据同步完成");
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void syncRecentVacationsAndTransferTasks() {
        log.info("开始增量休假同步及待办转派...");

        for (Map.Entry<String, VacationCalendarAdapter> entry : vacationAdapters.entrySet()) {
            VacationCalendarAdapter adapter = entry.getValue();
            try {
                if (adapter.isEnabled()) {
                    log.info("从{}增量同步休假数据", adapter.getSourceName());
                }
            } catch (Exception e) {
                log.error("增量同步休假失败, adapter={}, error={}", adapter.getSourceName(), e.getMessage());
            }
        }

        try {
            int count = approvalService.batchTransferVacationUsers();
            log.info("休假待办批量转派完成, 转派{}个任务", count);
        } catch (Exception e) {
            log.error("休假待办批量转派失败, error={}", e.getMessage(), e);
        }

        log.info("增量休假同步及待办转派完成");
    }
}
