package com.innerworkflow.bpmn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.bpmn.dto.WfCategoryQueryDTO;
import com.innerworkflow.bpmn.entity.WfCategory;
import com.innerworkflow.bpmn.mapper.WfCategoryMapper;
import com.innerworkflow.bpmn.service.WfCategoryService;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;

import java.util.List;

@Service
public class WfCategoryServiceImpl extends ServiceImpl<WfCategoryMapper, WfCategory> implements WfCategoryService {

    @Override
    public IPage<WfCategory> page(WfCategoryQueryDTO queryDTO) {
        LambdaQueryWrapper<WfCategory> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getBusinessLineId() != null) {
            wrapper.eq(WfCategory::getBusinessLineId, queryDTO.getBusinessLineId());
        }
        if (StrUtil.isNotBlank(queryDTO.getCategoryName())) {
            wrapper.like(WfCategory::getCategoryName, queryDTO.getCategoryName());
        }
        if (StrUtil.isNotBlank(queryDTO.getCategoryCode())) {
            wrapper.like(WfCategory::getCategoryCode, queryDTO.getCategoryCode());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(WfCategory::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByAsc(WfCategory::getSortOrder);
        return this.page(queryDTO.buildPage("sort_order asc"), wrapper);
    }

    @Override
    public List<WfCategory> listByBusinessLineId(Long businessLineId) {
        LambdaQueryWrapper<WfCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCategory::getBusinessLineId, businessLineId);
        wrapper.eq(WfCategory::getStatus, 1);
        wrapper.orderByAsc(WfCategory::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    public WfCategory getById(Long id) {
        return super.getById(id);
    }

    @Override
    public boolean save(WfCategory category) {
        return super.save(category);
    }

    @Override
    public boolean updateById(WfCategory category) {
        return super.updateById(category);
    }

    @Override
    public boolean removeById(Long id) {
        return super.removeById(id);
    }
}
