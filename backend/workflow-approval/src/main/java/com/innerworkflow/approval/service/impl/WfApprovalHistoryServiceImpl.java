package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.entity.WfApprovalHistory;
import com.innerworkflow.approval.mapper.WfApprovalHistoryMapper;
import com.innerworkflow.approval.service.WfApprovalHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfApprovalHistoryServiceImpl extends ServiceImpl<WfApprovalHistoryMapper, WfApprovalHistory> implements WfApprovalHistoryService {

    @Override
    public List<WfApprovalHistory> listByInstanceId(Long instanceId) {
        LambdaQueryWrapper<WfApprovalHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalHistory::getInstanceId, instanceId);
        wrapper.orderByAsc(WfApprovalHistory::getOperateTime);
        return this.list(wrapper);
    }

    @Override
    public List<WfApprovalHistory> listValidByInstanceId(Long instanceId) {
        LambdaQueryWrapper<WfApprovalHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalHistory::getInstanceId, instanceId);
        wrapper.eq(WfApprovalHistory::getIsValid, 1);
        wrapper.orderByAsc(WfApprovalHistory::getOperateTime);
        return this.list(wrapper);
    }

    @Override
    public void markInvalidByInstanceIdAndNodeId(Long instanceId, String nodeId) {
        LambdaUpdateWrapper<WfApprovalHistory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(WfApprovalHistory::getInstanceId, instanceId);
        wrapper.eq(WfApprovalHistory::getNodeId, nodeId);
        wrapper.set(WfApprovalHistory::getIsValid, 0);
        this.update(wrapper);
    }
}
