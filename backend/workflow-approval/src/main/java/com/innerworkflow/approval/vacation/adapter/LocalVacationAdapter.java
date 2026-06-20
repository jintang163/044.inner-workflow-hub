package com.innerworkflow.approval.vacation.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.innerworkflow.approval.entity.WfUserVacation;
import com.innerworkflow.approval.mapper.WfUserVacationMapper;
import com.innerworkflow.approval.vacation.VacationCalendarAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component("localVacationAdapter")
@RequiredArgsConstructor
public class LocalVacationAdapter implements VacationCalendarAdapter {

    private final WfUserVacationMapper userVacationMapper;

    @Override
    public String getSourceType() {
        return "1";
    }

    @Override
    public String getSourceName() {
        return "本地设置";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<WfUserVacation> syncUserVacation(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("本地休假适配器不提供同步功能, userId={}", userId);
        return Collections.emptyList();
    }

    @Override
    public List<WfUserVacation> syncUsersVacation(List<Long> userIds, LocalDateTime startTime, LocalDateTime endTime) {
        return Collections.emptyList();
    }

    @Override
    public boolean isUserOnVacation(Long userId, LocalDateTime time) {
        if (userId == null || time == null) {
            return false;
        }

        LambdaQueryWrapper<WfUserVacation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfUserVacation::getUserId, userId);
        wrapper.eq(WfUserVacation::getVacationStatus, 1);
        wrapper.le(WfUserVacation::getStartTime, time);
        wrapper.ge(WfUserVacation::getEndTime, time);
        wrapper.eq(WfUserVacation::getSourceType, 1);

        Long count = userVacationMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public WfUserVacation getCurrentVacation(Long userId) {
        if (userId == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<WfUserVacation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfUserVacation::getUserId, userId);
        wrapper.eq(WfUserVacation::getVacationStatus, 1);
        wrapper.le(WfUserVacation::getStartTime, now);
        wrapper.ge(WfUserVacation::getEndTime, now);
        wrapper.eq(WfUserVacation::getSourceType, 1);
        wrapper.orderByAsc(WfUserVacation::getStartTime);
        wrapper.last("LIMIT 1");

        return userVacationMapper.selectOne(wrapper);
    }
}
