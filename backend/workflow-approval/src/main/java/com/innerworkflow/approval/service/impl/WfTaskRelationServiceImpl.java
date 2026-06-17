package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.entity.WfTaskRelation;
import com.innerworkflow.approval.mapper.WfTaskRelationMapper;
import com.innerworkflow.approval.service.WfTaskRelationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfTaskRelationServiceImpl extends ServiceImpl<WfTaskRelationMapper, WfTaskRelation> implements WfTaskRelationService {

    @Override
    public List<WfTaskRelation> listByParentTaskId(Long parentTaskId) {
        LambdaQueryWrapper<WfTaskRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTaskRelation::getParentTaskId, parentTaskId);
        wrapper.orderByDesc(WfTaskRelation::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<WfTaskRelation> listByChildTaskId(Long childTaskId) {
        LambdaQueryWrapper<WfTaskRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTaskRelation::getChildTaskId, childTaskId);
        return this.list(wrapper);
    }
}
