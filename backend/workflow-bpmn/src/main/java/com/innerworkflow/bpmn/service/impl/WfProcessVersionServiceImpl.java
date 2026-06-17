package com.innerworkflow.bpmn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.mapper.WfProcessVersionMapper;
import com.innerworkflow.bpmn.service.WfProcessVersionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfProcessVersionServiceImpl extends ServiceImpl<WfProcessVersionMapper, WfProcessVersion> implements WfProcessVersionService {

    @Override
    public List<WfProcessVersion> listByProcessDefinitionId(Long processDefinitionId) {
        LambdaQueryWrapper<WfProcessVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessVersion::getProcessDefinitionId, processDefinitionId);
        wrapper.orderByDesc(WfProcessVersion::getVersion);
        return this.list(wrapper);
    }

    @Override
    public WfProcessVersion getById(Long id) {
        return super.getById(id);
    }

    @Override
    public WfProcessVersion getCurrentVersion(Long processDefinitionId) {
        LambdaQueryWrapper<WfProcessVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessVersion::getProcessDefinitionId, processDefinitionId);
        wrapper.eq(WfProcessVersion::getIsCurrent, 1);
        return this.getOne(wrapper);
    }

    @Override
    public WfProcessVersion getByVersion(Long processDefinitionId, Integer version) {
        LambdaQueryWrapper<WfProcessVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessVersion::getProcessDefinitionId, processDefinitionId);
        wrapper.eq(WfProcessVersion::getVersion, version);
        return this.getOne(wrapper);
    }

    @Override
    public boolean save(WfProcessVersion processVersion) {
        return super.save(processVersion);
    }

    @Override
    public boolean updateById(WfProcessVersion processVersion) {
        return super.updateById(processVersion);
    }
}
