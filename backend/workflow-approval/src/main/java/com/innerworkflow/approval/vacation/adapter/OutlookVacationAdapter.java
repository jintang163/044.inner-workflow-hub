package com.innerworkflow.approval.vacation.adapter;

import com.innerworkflow.approval.entity.WfUserVacation;
import com.innerworkflow.approval.vacation.VacationCalendarAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component("outlookVacationAdapter")
@ConditionalOnProperty(name = "workflow.vacation.outlook.enabled", havingValue = "true", matchIfMissing = false)
public class OutlookVacationAdapter implements VacationCalendarAdapter {

    @Override
    public String getSourceType() {
        return "4";
    }

    @Override
    public String getSourceName() {
        return "Outlook";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<WfUserVacation> syncUserVacation(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.warn("Outlook休假同步暂未实现, userId={}", userId);
        return Collections.emptyList();
    }

    @Override
    public List<WfUserVacation> syncUsersVacation(List<Long> userIds, LocalDateTime startTime, LocalDateTime endTime) {
        log.warn("Outlook批量休假同步暂未实现, userCount={}", userIds != null ? userIds.size() : 0);
        return Collections.emptyList();
    }

    @Override
    public boolean isUserOnVacation(Long userId, LocalDateTime time) {
        log.warn("Outlook休假状态查询暂未实现, userId={}", userId);
        return false;
    }

    @Override
    public WfUserVacation getCurrentVacation(Long userId) {
        log.warn("Outlook当前休假查询暂未实现, userId={}", userId);
        return null;
    }
}
