package com.innerworkflow.approval.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfUserVacationQueryDTO;
import com.innerworkflow.approval.dto.WfUserVacationSaveDTO;
import com.innerworkflow.approval.entity.WfUserVacation;
import com.innerworkflow.approval.mapper.WfUserVacationMapper;
import com.innerworkflow.approval.service.WfUserVacationService;
import com.innerworkflow.approval.vacation.VacationCalendarAdapter;
import com.innerworkflow.common.context.TenantContext;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfUserVacationServiceImpl extends ServiceImpl<WfUserVacationMapper, WfUserVacation> implements WfUserVacationService {

    private final Map<String, VacationCalendarAdapter> vacationAdapters;

    @Override
    public IPage<WfUserVacation> page(WfUserVacationQueryDTO queryDTO) {
        LambdaQueryWrapper<WfUserVacation> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(WfUserVacation::getStartTime);
        Page<WfUserVacation> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return this.page(page, wrapper);
    }

    @Override
    public WfUserVacation getById(Long id) {
        return super.getById(id);
    }

    @Override
    public boolean save(WfUserVacationSaveDTO dto) {
        validateTimeRange(dto.getStartTime(), dto.getEndTime());

        WfUserVacation vacation = new WfUserVacation();
        BeanUtils.copyProperties(dto, vacation);
        vacation.setSourceType(1);
        vacation.setVacationStatus(1);
        vacation.setTenantId(TenantContext.getTenantId());
        vacation.setCreateBy(SecurityUtils.getCurrentUserId());
        vacation.setCreateTime(LocalDateTime.now());
        vacation.setUpdateBy(SecurityUtils.getCurrentUserId());
        vacation.setUpdateTime(LocalDateTime.now());

        if (vacation.getFullDay() == null) {
            vacation.setFullDay(1);
        }
        if (vacation.getAutoDelegate() == null) {
            vacation.setAutoDelegate(1);
        }

        return this.save(vacation);
    }

    @Override
    public boolean update(WfUserVacationSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        validateTimeRange(dto.getStartTime(), dto.getEndTime());

        WfUserVacation vacation = new WfUserVacation();
        BeanUtils.copyProperties(dto, vacation);
        vacation.setUpdateBy(SecurityUtils.getCurrentUserId());
        vacation.setUpdateTime(LocalDateTime.now());

        return this.updateById(vacation);
    }

    @Override
    public boolean deleteById(Long id) {
        return this.removeById(id);
    }

    @Override
    public boolean cancelVacation(Long id) {
        WfUserVacation vacation = this.getById(id);
        if (vacation == null) {
            throw new BusinessException("休假记录不存在");
        }
        vacation.setVacationStatus(0);
        vacation.setUpdateBy(SecurityUtils.getCurrentUserId());
        vacation.setUpdateTime(LocalDateTime.now());
        return this.updateById(vacation);
    }

    @Override
    public WfUserVacation getCurrentVacation(Long userId) {
        if (userId == null) {
            return null;
        }

        List<WfUserVacation> allCurrentVacations = new ArrayList<>();

        for (VacationCalendarAdapter adapter : vacationAdapters.values()) {
            try {
                if (adapter.isEnabled()) {
                    WfUserVacation vacation = adapter.getCurrentVacation(userId);
                    if (vacation != null) {
                        allCurrentVacations.add(vacation);
                    }
                }
            } catch (Exception e) {
                log.warn("获取当前休假失败, adapter={}, userId={}, error={}",
                        adapter.getSourceName(), userId, e.getMessage());
            }
        }

        if (allCurrentVacations.isEmpty()) {
            return null;
        }

        allCurrentVacations.sort(Comparator.comparing(WfUserVacation::getStartTime));
        return allCurrentVacations.get(0);
    }

    @Override
    public boolean isUserOnVacation(Long userId, LocalDateTime time) {
        if (userId == null || time == null) {
            return false;
        }

        for (VacationCalendarAdapter adapter : vacationAdapters.values()) {
            try {
                if (adapter.isEnabled() && adapter.isUserOnVacation(userId, time)) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("检查休假状态失败, adapter={}, userId={}, error={}",
                        adapter.getSourceName(), userId, e.getMessage());
            }
        }

        return false;
    }

    @Override
    public List<WfUserVacation> listByUserAndTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<WfUserVacation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfUserVacation::getUserId, userId);
        wrapper.eq(WfUserVacation::getVacationStatus, 1);
        if (startTime != null) {
            wrapper.ge(WfUserVacation::getEndTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(WfUserVacation::getStartTime, endTime);
        }
        wrapper.orderByAsc(WfUserVacation::getStartTime);
        return this.list(wrapper);
    }

    @Override
    public void syncVacationFromSource(Long userId, String sourceType) {
        VacationCalendarAdapter adapter = getAdapterBySourceType(sourceType);
        if (adapter == null || !adapter.isEnabled()) {
            log.warn("指定的休假数据源未启用或不存在, sourceType={}", sourceType);
            return;
        }

        try {
            LocalDateTime startTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(30);

            List<WfUserVacation> vacations = adapter.syncUserVacation(userId, startTime, endTime);
            if (vacations != null && !vacations.isEmpty()) {
                saveOrUpdateSyncVacations(vacations, sourceType);
                log.info("从{}同步用户休假成功, userId={}, 共{}条", adapter.getSourceName(), userId, vacations.size());
            }
        } catch (Exception e) {
            log.error("从{}同步用户休假失败, userId={}, error={}", adapter.getSourceName(), userId, e.getMessage(), e);
        }
    }

    @Override
    public void syncAllVacations(String sourceType) {
        // 可扩展：从用户系统获取所有用户，批量同步
        log.warn("全量休假同步暂未完全实现, sourceType={}", sourceType);
    }

    private VacationCalendarAdapter getAdapterBySourceType(String sourceType) {
        if (StrUtil.isBlank(sourceType)) {
            return null;
        }
        for (VacationCalendarAdapter adapter : vacationAdapters.values()) {
            if (sourceType.equals(adapter.getSourceType())) {
                return adapter;
            }
        }
        return null;
    }

    private void saveOrUpdateSyncVacations(List<WfUserVacation> vacations, String sourceType) {
        for (WfUserVacation vacation : vacations) {
            try {
                LambdaQueryWrapper<WfUserVacation> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(WfUserVacation::getSourceType, sourceType);
                wrapper.eq(WfUserVacation::getSourceId, vacation.getSourceId());
                WfUserVacation existing = this.getOne(wrapper);

                if (existing != null) {
                    vacation.setId(existing.getId());
                    vacation.setUpdateTime(LocalDateTime.now());
                    this.updateById(vacation);
                } else {
                    vacation.setCreateTime(LocalDateTime.now());
                    vacation.setUpdateTime(LocalDateTime.now());
                    this.save(vacation);
                }
            } catch (Exception e) {
                log.error("保存同步休假记录失败, sourceId={}, error={}", vacation.getSourceId(), e.getMessage(), e);
            }
        }
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("开始时间和结束时间不能为空");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
    }

    private LambdaQueryWrapper<WfUserVacation> buildQueryWrapper(WfUserVacationQueryDTO queryDTO) {
        LambdaQueryWrapper<WfUserVacation> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getUserId() != null) {
            wrapper.eq(WfUserVacation::getUserId, queryDTO.getUserId());
        }
        if (queryDTO.getVacationType() != null) {
            wrapper.eq(WfUserVacation::getVacationType, queryDTO.getVacationType());
        }
        if (queryDTO.getSourceType() != null) {
            wrapper.eq(WfUserVacation::getSourceType, queryDTO.getSourceType());
        }
        if (queryDTO.getVacationStatus() != null) {
            wrapper.eq(WfUserVacation::getVacationStatus, queryDTO.getVacationStatus());
        }
        if (queryDTO.getStartTime() != null) {
            wrapper.ge(WfUserVacation::getEndTime, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(WfUserVacation::getStartTime, queryDTO.getEndTime());
        }

        return wrapper;
    }
}
