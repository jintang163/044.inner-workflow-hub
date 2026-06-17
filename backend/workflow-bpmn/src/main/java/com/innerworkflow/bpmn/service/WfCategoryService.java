package com.innerworkflow.bpmn.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.bpmn.dto.WfCategoryQueryDTO;
import com.innerworkflow.bpmn.entity.WfCategory;

import java.util.List;

public interface WfCategoryService {

    IPage<WfCategory> page(WfCategoryQueryDTO queryDTO);

    List<WfCategory> listByBusinessLineId(Long businessLineId);

    WfCategory getById(Long id);

    boolean save(WfCategory category);

    boolean updateById(WfCategory category);

    boolean removeById(Long id);
}
