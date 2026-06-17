package com.innerworkflow.bpmn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.bpmn.dto.WfBusinessLineQueryDTO;
import com.innerworkflow.bpmn.entity.WfBusinessLine;
import com.innerworkflow.bpmn.mapper.WfBusinessLineMapper;
import com.innerworkflow.bpmn.service.WfBusinessLineService;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;

import java.util.List;

@Service
public class WfBusinessLineServiceImpl extends ServiceImpl<WfBusinessLineMapper, WfBusinessLine> implements WfBusinessLineService {

    @Override
    public IPage<WfBusinessLine> page(WfBusinessLineQueryDTO queryDTO) {
        LambdaQueryWrapper<WfBusinessLine> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(queryDTO.getLineName())) {
            wrapper.like(WfBusinessLine::getLineName, queryDTO.getLineName());
        }
        if (StrUtil.isNotBlank(queryDTO.getLineCode())) {
            wrapper.like(WfBusinessLine::getLineCode, queryDTO.getLineCode());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(WfBusinessLine::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByAsc(WfBusinessLine::getSortOrder);
        return this.page(queryDTO.buildPage("sort_order asc"), wrapper);
    }

    @Override
    public List<WfBusinessLine> listAll() {
        LambdaQueryWrapper<WfBusinessLine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfBusinessLine::getStatus, 1);
        wrapper.orderByAsc(WfBusinessLine::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    public WfBusinessLine getById(Long id) {
        return super.getById(id);
    }

    @Override
    public boolean save(WfBusinessLine businessLine) {
        return super.save(businessLine);
    }

    @Override
    public boolean updateById(WfBusinessLine businessLine) {
        return super.updateById(businessLine);
    }

    @Override
    public boolean removeById(Long id) {
        return super.removeById(id);
    }
}
