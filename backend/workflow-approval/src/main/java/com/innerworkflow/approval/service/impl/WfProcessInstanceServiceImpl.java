package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfMyProcessQueryDTO;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.mapper.WfProcessInstanceMapper;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.vo.WfProcessInstanceVO;
import com.innerworkflow.common.enums.InstanceStatusEnum;
import com.innerworkflow.common.util.SecurityUtils;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfProcessInstanceServiceImpl extends ServiceImpl<WfProcessInstanceMapper, WfProcessInstance> implements WfProcessInstanceService {

    @Override
    public IPage<WfProcessInstanceVO> pageMyProcess(WfMyProcessQueryDTO queryDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<WfProcessInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessInstance::getStartUserId, userId);
        if (StrUtil.isNotBlank(queryDTO.getProcessKey())) {
            wrapper.eq(WfProcessInstance::getProcessKey, queryDTO.getProcessKey());
        }
        if (queryDTO.getBusinessLineId() != null) {
            wrapper.eq(WfProcessInstance::getBusinessLineId, queryDTO.getBusinessLineId());
        }
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(WfProcessInstance::getCategoryId, queryDTO.getCategoryId());
        }
        if (StrUtil.isNotBlank(queryDTO.getTitle())) {
            wrapper.like(WfProcessInstance::getTitle, queryDTO.getTitle());
        }
        if (queryDTO.getInstanceStatus() != null) {
            wrapper.eq(WfProcessInstance::getInstanceStatus, queryDTO.getInstanceStatus());
        }
        wrapper.orderByDesc(WfProcessInstance::getCreateTime);
        return this.page(queryDTO.buildPage("create_time desc"), wrapper)
                .convert(this::convertToVO);
    }

    @Override
    public WfProcessInstance getByFlowableInstId(String flowableInstId) {
        LambdaQueryWrapper<WfProcessInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessInstance::getFlowableProcessInstId, flowableInstId);
        return this.getOne(wrapper);
    }

    @Override
    public WfProcessInstance getByInstanceNo(String instanceNo) {
        LambdaQueryWrapper<WfProcessInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessInstance::getInstanceNo, instanceNo);
        return this.getOne(wrapper);
    }

    @Override
    public WfProcessInstanceVO getDetailById(Long id) {
        WfProcessInstance instance = this.getById(id);
        if (instance == null) {
            return null;
        }
        return convertToVO(instance);
    }

    private WfProcessInstanceVO convertToVO(WfProcessInstance instance) {
        WfProcessInstanceVO vo = new WfProcessInstanceVO();
        BeanUtils.copyProperties(instance, vo);
        InstanceStatusEnum statusEnum = InstanceStatusEnum.getByCode(instance.getInstanceStatus());
        if (statusEnum != null) {
            vo.setInstanceStatusName(statusEnum.getDesc());
        }
        return vo;
    }

    @Override
    public List<WfProcessInstance> listByProcessKey(String processKey) {
        LambdaQueryWrapper<WfProcessInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessInstance::getProcessKey, processKey);
        wrapper.orderByDesc(WfProcessInstance::getCreateTime);
        return this.list(wrapper);
    }
}
