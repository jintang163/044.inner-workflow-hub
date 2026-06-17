package com.innerworkflow.bpmn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.bpmn.dto.WfProcessDefinitionQueryDTO;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;
import com.innerworkflow.bpmn.mapper.WfProcessDefinitionMapper;
import com.innerworkflow.bpmn.service.WfProcessDefinitionService;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;

@Service
public class WfProcessDefinitionServiceImpl extends ServiceImpl<WfProcessDefinitionMapper, WfProcessDefinition> implements WfProcessDefinitionService {

    @Override
    public IPage<WfProcessDefinition> page(WfProcessDefinitionQueryDTO queryDTO) {
        LambdaQueryWrapper<WfProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getBusinessLineId() != null) {
            wrapper.eq(WfProcessDefinition::getBusinessLineId, queryDTO.getBusinessLineId());
        }
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(WfProcessDefinition::getCategoryId, queryDTO.getCategoryId());
        }
        if (StrUtil.isNotBlank(queryDTO.getProcessName())) {
            wrapper.like(WfProcessDefinition::getProcessName, queryDTO.getProcessName());
        }
        if (StrUtil.isNotBlank(queryDTO.getProcessKey())) {
            wrapper.like(WfProcessDefinition::getProcessKey, queryDTO.getProcessKey());
        }
        if (queryDTO.getProcessStatus() != null) {
            wrapper.eq(WfProcessDefinition::getProcessStatus, queryDTO.getProcessStatus());
        }
        wrapper.orderByDesc(WfProcessDefinition::getCreateTime);
        return this.page(queryDTO.buildPage("create_time desc"), wrapper);
    }

    @Override
    public WfProcessDefinition getById(Long id) {
        return super.getById(id);
    }

    @Override
    public WfProcessDefinition getByProcessKey(String processKey) {
        LambdaQueryWrapper<WfProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessDefinition::getProcessKey, processKey);
        return this.getOne(wrapper);
    }

    @Override
    public boolean save(WfProcessDefinition processDefinition) {
        return super.save(processDefinition);
    }

    @Override
    public boolean updateById(WfProcessDefinition processDefinition) {
        return super.updateById(processDefinition);
    }

    @Override
    public boolean removeById(Long id) {
        return super.removeById(id);
    }
}
