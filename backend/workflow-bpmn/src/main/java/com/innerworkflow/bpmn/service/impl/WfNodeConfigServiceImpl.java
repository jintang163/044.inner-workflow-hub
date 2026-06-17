package com.innerworkflow.bpmn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.mapper.WfNodeConfigMapper;
import com.innerworkflow.bpmn.service.WfNodeConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfNodeConfigServiceImpl extends ServiceImpl<WfNodeConfigMapper, WfNodeConfig> implements WfNodeConfigService {

    @Override
    public List<WfNodeConfig> listByProcessVersionId(Long processVersionId) {
        LambdaQueryWrapper<WfNodeConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfNodeConfig::getProcessVersionId, processVersionId);
        wrapper.orderByAsc(WfNodeConfig::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    public WfNodeConfig getByNodeId(Long processVersionId, String nodeId) {
        LambdaQueryWrapper<WfNodeConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfNodeConfig::getProcessVersionId, processVersionId);
        wrapper.eq(WfNodeConfig::getNodeId, nodeId);
        return this.getOne(wrapper);
    }

    @Override
    public boolean saveBatch(List<WfNodeConfig> nodeConfigs) {
        return super.saveBatch(nodeConfigs);
    }

    @Override
    public boolean removeByProcessVersionId(Long processVersionId) {
        LambdaQueryWrapper<WfNodeConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfNodeConfig::getProcessVersionId, processVersionId);
        return this.remove(wrapper);
    }
}
