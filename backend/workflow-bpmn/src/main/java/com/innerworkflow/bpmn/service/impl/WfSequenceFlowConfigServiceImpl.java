package com.innerworkflow.bpmn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.bpmn.entity.WfSequenceFlowConfig;
import com.innerworkflow.bpmn.mapper.WfSequenceFlowConfigMapper;
import com.innerworkflow.bpmn.service.WfSequenceFlowConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfSequenceFlowConfigServiceImpl extends ServiceImpl<WfSequenceFlowConfigMapper, WfSequenceFlowConfig> implements WfSequenceFlowConfigService {

    @Override
    public List<WfSequenceFlowConfig> listByProcessVersionId(Long processVersionId) {
        LambdaQueryWrapper<WfSequenceFlowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfSequenceFlowConfig::getProcessVersionId, processVersionId);
        wrapper.orderByAsc(WfSequenceFlowConfig::getConditionPriority);
        return this.list(wrapper);
    }

    @Override
    public List<WfSequenceFlowConfig> listBySourceNodeId(Long processVersionId, String sourceNodeId) {
        LambdaQueryWrapper<WfSequenceFlowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfSequenceFlowConfig::getProcessVersionId, processVersionId);
        wrapper.eq(WfSequenceFlowConfig::getSourceNodeId, sourceNodeId);
        wrapper.orderByAsc(WfSequenceFlowConfig::getConditionPriority);
        return this.list(wrapper);
    }

    @Override
    public boolean saveBatch(List<WfSequenceFlowConfig> sequenceFlowConfigs) {
        return super.saveBatch(sequenceFlowConfigs);
    }

    @Override
    public boolean removeByProcessVersionId(Long processVersionId) {
        LambdaQueryWrapper<WfSequenceFlowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfSequenceFlowConfig::getProcessVersionId, processVersionId);
        return this.remove(wrapper);
    }
}
