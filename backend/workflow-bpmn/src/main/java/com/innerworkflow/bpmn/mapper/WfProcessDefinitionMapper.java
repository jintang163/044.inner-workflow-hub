package com.innerworkflow.bpmn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfProcessDefinitionMapper extends BaseMapper<WfProcessDefinition> {
}
