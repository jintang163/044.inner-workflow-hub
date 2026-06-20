package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfUserVacationQueryDTO;
import com.innerworkflow.approval.dto.WfUserVacationSaveDTO;
import com.innerworkflow.approval.entity.WfUserVacation;

import java.time.LocalDateTime;
import java.util.List;

public interface WfUserVacationService {

    IPage<WfUserVacation> page(WfUserVacationQueryDTO queryDTO);

    WfUserVacation getById(Long id);

    boolean save(WfUserVacationSaveDTO dto);

    boolean update(WfUserVacationSaveDTO dto);

    boolean deleteById(Long id);

    boolean cancelVacation(Long id);

    WfUserVacation getCurrentVacation(Long userId);

    boolean isUserOnVacation(Long userId, LocalDateTime time);

    List<WfUserVacation> listByUserAndTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    void syncVacationFromSource(Long userId, String sourceType);

    void syncAllVacations(String sourceType);
}
