package com.innerworkflow.bpmn.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.bpmn.dto.WfProcessDefinitionQueryDTO;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;

public interface WfProcessDefinitionService {

    IPage<WfProcessDefinition> page(WfProcessDefinitionQueryDTO queryDTO);

    WfProcessDefinition getById(Long id);

    WfProcessDefinition getByProcessKey(String processKey);

    boolean save(WfProcessDefinition processDefinition);

    boolean updateById(WfProcessDefinition processDefinition);

    boolean removeById(Long id);
}
