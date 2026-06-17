package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfCcTaskQueryDTO;
import com.innerworkflow.approval.entity.WfCcTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.mapper.WfCcTaskMapper;
import com.innerworkflow.approval.service.WfCcTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.vo.WfCcTaskVO;
import com.innerworkflow.common.util.SecurityUtils;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WfCcTaskServiceImpl extends ServiceImpl<WfCcTaskMapper, WfCcTask> implements WfCcTaskService {

    private final WfProcessInstanceService processInstanceService;

    @Override
    public IPage<WfCcTaskVO> page(WfCcTaskQueryDTO queryDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<WfCcTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCcTask::getCcUserId, userId);
        if (StrUtil.isNotBlank(queryDTO.getProcessKey())) {
            wrapper.eq(WfCcTask::getProcessKey, queryDTO.getProcessKey());
        }
        if (queryDTO.getIsRead() != null) {
            wrapper.eq(WfCcTask::getIsRead, queryDTO.getIsRead());
        }
        wrapper.orderByDesc(WfCcTask::getCcTime);
        return this.page(queryDTO.buildPage("cc_time desc"), wrapper)
                .convert(this::convertToVO);
    }

    @Override
    public List<WfCcTask> listByInstanceId(Long instanceId) {
        LambdaQueryWrapper<WfCcTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCcTask::getInstanceId, instanceId);
        return this.list(wrapper);
    }

    @Override
    public boolean markRead(Long id) {
        LambdaUpdateWrapper<WfCcTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(WfCcTask::getId, id);
        wrapper.eq(WfCcTask::getCcUserId, SecurityUtils.getCurrentUserId());
        wrapper.set(WfCcTask::getIsRead, 1);
        wrapper.set(WfCcTask::getReadTime, LocalDateTime.now());
        return this.update(wrapper);
    }

    @Override
    public long countUnreadByUserId(Long userId) {
        LambdaQueryWrapper<WfCcTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCcTask::getCcUserId, userId);
        wrapper.eq(WfCcTask::getIsRead, 0);
        return this.count(wrapper);
    }

    private WfCcTaskVO convertToVO(WfCcTask ccTask) {
        WfCcTaskVO vo = new WfCcTaskVO();
        BeanUtils.copyProperties(ccTask, vo);
        WfProcessInstance instance = processInstanceService.getById(ccTask.getInstanceId());
        if (instance != null) {
            vo.setTitle(instance.getTitle());
            vo.setStartUserId(instance.getStartUserId());
            vo.setInstanceNo(instance.getInstanceNo());
            vo.setInstanceStatus(instance.getInstanceStatus());
        }
        return vo;
    }
}
