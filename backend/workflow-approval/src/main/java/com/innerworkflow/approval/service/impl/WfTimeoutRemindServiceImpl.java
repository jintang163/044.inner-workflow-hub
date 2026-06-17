package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.entity.WfTimeoutRemind;
import com.innerworkflow.approval.mapper.WfTimeoutRemindMapper;
import com.innerworkflow.approval.service.WfTimeoutRemindService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfTimeoutRemindServiceImpl extends ServiceImpl<WfTimeoutRemindMapper, WfTimeoutRemind> implements WfTimeoutRemindService {

    @Override
    public List<WfTimeoutRemind> listByTaskId(Long taskId) {
        LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTimeoutRemind::getTaskId, taskId);
        wrapper.orderByDesc(WfTimeoutRemind::getRemindTime);
        return this.list(wrapper);
    }

    @Override
    public int countByTaskId(Long taskId) {
        LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTimeoutRemind::getTaskId, taskId);
        return (int) this.count(wrapper);
    }

    @Override
    public List<WfTimeoutRemind> listByInstanceId(Long instanceId) {
        LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTimeoutRemind::getInstanceId, instanceId);
        wrapper.orderByDesc(WfTimeoutRemind::getRemindTime);
        return this.list(wrapper);
    }
}
