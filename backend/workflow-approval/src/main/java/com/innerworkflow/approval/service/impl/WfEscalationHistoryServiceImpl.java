package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.entity.WfEscalationHistory;
import com.innerworkflow.approval.mapper.WfEscalationHistoryMapper;
import com.innerworkflow.approval.service.WfEscalationHistoryService;
import com.innerworkflow.common.dto.PageQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfEscalationHistoryServiceImpl extends ServiceImpl<WfEscalationHistoryMapper, WfEscalationHistory> implements WfEscalationHistoryService {

    @Override
    public List<WfEscalationHistory> listByInstanceId(Long instanceId) {
        LambdaQueryWrapper<WfEscalationHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfEscalationHistory::getInstanceId, instanceId);
        wrapper.orderByDesc(WfEscalationHistory::getTriggerTime);
        return this.list(wrapper);
    }

    @Override
    public List<WfEscalationHistory> listByTaskId(Long taskId) {
        LambdaQueryWrapper<WfEscalationHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfEscalationHistory::getTaskId, taskId);
        wrapper.orderByDesc(WfEscalationHistory::getTriggerTime);
        return this.list(wrapper);
    }

    @Override
    public IPage<WfEscalationHistory> pageByInstanceId(Long instanceId, PageQuery query) {
        LambdaQueryWrapper<WfEscalationHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfEscalationHistory::getInstanceId, instanceId);
        wrapper.orderByDesc(WfEscalationHistory::getTriggerTime);
        Page<WfEscalationHistory> page = new Page<>(query.getPageNum(), query.getPageSize());
        return this.page(page, wrapper);
    }

    @Override
    public IPage<WfEscalationHistory> pageByTaskId(Long taskId, PageQuery query) {
        LambdaQueryWrapper<WfEscalationHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfEscalationHistory::getTaskId, taskId);
        wrapper.orderByDesc(WfEscalationHistory::getTriggerTime);
        Page<WfEscalationHistory> page = new Page<>(query.getPageNum(), query.getPageSize());
        return this.page(page, wrapper);
    }

    @Override
    public boolean save(WfEscalationHistory history) {
        return super.save(history);
    }
}
