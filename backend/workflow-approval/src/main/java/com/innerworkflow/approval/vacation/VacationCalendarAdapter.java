package com.innerworkflow.approval.vacation;

import com.innerworkflow.approval.entity.WfUserVacation;

import java.time.LocalDateTime;
import java.util.List;

public interface VacationCalendarAdapter {

    String getSourceType();

    String getSourceName();

    boolean isEnabled();

    List<WfUserVacation> syncUserVacation(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    List<WfUserVacation> syncUsersVacation(List<Long> userIds, LocalDateTime startTime, LocalDateTime endTime);

    boolean isUserOnVacation(Long userId, LocalDateTime time);

    WfUserVacation getCurrentVacation(Long userId);
}
