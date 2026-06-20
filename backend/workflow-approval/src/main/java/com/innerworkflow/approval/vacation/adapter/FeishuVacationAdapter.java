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
@Component("feishuVacationAdapter")
@ConditionalOnProperty(name = "workflow.vacation.feishu.enabled", havingValue = "true", matchIfMissing = false)
public class FeishuVacationAdapter implements VacationCalendarAdapter {

    @Override
    public String getSourceType() {
        return "3";
    }

    @Override
    public String getSourceName() {
        return "飞书";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<WfUserVacation> syncUserVacation(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.warn("飞书休假同步暂未实现, userId={}", userId);
        return Collections.emptyList();
    }

    @Override
    public List<WfUserVacation> syncUsersVacation(List<Long> userIds, LocalDateTime startTime, LocalDateTime endTime) {
        log.warn("飞书批量休假同步暂未实现, userCount={}", userIds != null ? userIds.size() : 0);
        return Collections.emptyList();
    }

    @Override
    public boolean isUserOnVacation(Long userId, LocalDateTime time) {
        log.warn("飞书休假状态查询暂未实现, userId={}", userId);
        return false;
    }

    @Override
    public WfUserVacation getCurrentVacation(Long userId) {
        log.warn("飞书当前休假查询暂未实现, userId={}", userId);
        return null;
    }
}
