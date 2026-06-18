package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfCcAddDTO;
import com.innerworkflow.approval.dto.WfCcRemindDTO;
import com.innerworkflow.approval.dto.WfCcTaskQueryDTO;
import com.innerworkflow.approval.entity.WfCcTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.enums.CcTypeEnum;
import com.innerworkflow.approval.mapper.WfCcTaskMapper;
import com.innerworkflow.approval.service.WfCcTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.vo.WfCcTaskVO;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.common.config.FrontendConfig;
import com.innerworkflow.common.enums.InstanceStatusEnum;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.enums.EventTypeEnum;
import com.innerworkflow.notify.service.WfNotifyService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfCcTaskServiceImpl extends ServiceImpl<WfCcTaskMapper, WfCcTask> implements WfCcTaskService {

    private final WfProcessInstanceService processInstanceService;
    private final SysUserService sysUserService;
    private final WfNotifyService notifyService;
    private final FrontendConfig frontendConfig;

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
    public List<WfCcTaskVO> listVOByInstanceId(Long instanceId) {
        WfProcessInstance instance = processInstanceService.getById(instanceId);
        if (instance == null) {
            throw BusinessException.notFound("流程实例不存在");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(instance.getStartUserId())) {
            throw BusinessException.forbidden("只有发起人可以查看抄送列表");
        }

        return listAllVOsByInstanceId(instanceId);
    }

    @Override
    public List<WfCcTaskVO> listAllVOsByInstanceId(Long instanceId) {
        LambdaQueryWrapper<WfCcTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCcTask::getInstanceId, instanceId);
        wrapper.orderByDesc(WfCcTask::getCcTime);
        List<WfCcTask> list = this.list(wrapper);

        Set<Long> userIds = list.stream().map(WfCcTask::getCcUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserService.listByIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        }

        List<WfCcTaskVO> voList = new ArrayList<>();
        for (WfCcTask ccTask : list) {
            WfCcTaskVO vo = convertToVO(ccTask);
            SysUser user = userMap.get(ccTask.getCcUserId());
            if (user != null) {
                vo.setCcUserName(user.getRealName());
            }
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public long countUnreadByInstanceId(Long instanceId) {
        LambdaQueryWrapper<WfCcTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCcTask::getInstanceId, instanceId);
        wrapper.eq(WfCcTask::getIsRead, 0);
        return this.count(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCc(WfCcAddDTO addDTO) {
        WfProcessInstance instance = processInstanceService.getById(addDTO.getInstanceId());
        if (instance == null) {
            throw BusinessException.notFound("流程实例不存在");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(instance.getStartUserId())) {
            throw BusinessException.forbidden("只有发起人可以添加抄送人");
        }

        if (InstanceStatusEnum.CANCELED.getCode().equals(instance.getInstanceStatus())) {
            throw BusinessException.paramError("流程已取消，无法添加抄送");
        }

        doAddCc(addDTO, instance, CcTypeEnum.MANUAL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCcInternal(WfCcAddDTO addDTO) {
        WfProcessInstance instance = processInstanceService.getById(addDTO.getInstanceId());
        if (instance == null) {
            log.warn("内部添加抄送-流程实例不存在, instanceId={}", addDTO.getInstanceId());
            return;
        }
        CcTypeEnum ccType = addDTO.getCcType() != null
                ? CcTypeEnum.getByCode(addDTO.getCcType())
                : CcTypeEnum.MANUAL;
        if (ccType == null) {
            ccType = CcTypeEnum.MANUAL;
        }
        doAddCc(addDTO, instance, ccType);
    }

    private void doAddCc(WfCcAddDTO addDTO, WfProcessInstance instance, CcTypeEnum ccType) {
        if (addDTO.getCcUserIds() == null || addDTO.getCcUserIds().isEmpty()) {
            return;
        }

        Set<Long> existingCcUserIds = listByInstanceId(addDTO.getInstanceId()).stream()
                .map(WfCcTask::getCcUserId)
                .collect(Collectors.toSet());

        List<Long> newCcUserIds = addDTO.getCcUserIds().stream()
                .filter(userId -> !existingCcUserIds.contains(userId))
                .filter(userId -> !userId.equals(instance.getStartUserId()))
                .collect(Collectors.toList());

        if (newCcUserIds.isEmpty()) {
            log.info("没有需要添加的新抄送人, instanceId={}, ccType={}", addDTO.getInstanceId(), ccType.getName());
            return;
        }

        String detailUrl = frontendConfig.getApprovalDetailUrl(addDTO.getInstanceId());
        List<WfCcTask> ccTasks = new ArrayList<>();
        for (Long ccUserId : newCcUserIds) {
            WfCcTask ccTask = new WfCcTask();
            ccTask.setInstanceId(addDTO.getInstanceId());
            ccTask.setProcessKey(instance.getProcessKey());
            ccTask.setCcUserId(ccUserId);
            ccTask.setNodeId(addDTO.getNodeId());
            ccTask.setNodeName(addDTO.getNodeName());
            ccTask.setCcType(ccType.getCode());
            ccTask.setIsRead(0);
            ccTask.setCcTime(LocalDateTime.now());
            ccTask.setRemindCount(0);
            ccTask.setDetailUrl(detailUrl);
            ccTasks.add(ccTask);
        }
        this.saveBatch(ccTasks);
        log.info("添加抄送成功, instanceId={}, ccType={}, ccUserCount={}",
                addDTO.getInstanceId(), ccType.getName(), newCcUserIds.size());

        for (WfCcTask ccTask : ccTasks) {
            sendCcNotifyAsync(ccTask, instance);
        }
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
    public boolean markReadBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaUpdateWrapper<WfCcTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(WfCcTask::getId, ids);
        wrapper.eq(WfCcTask::getCcUserId, userId);
        wrapper.set(WfCcTask::getIsRead, 1);
        wrapper.set(WfCcTask::getReadTime, LocalDateTime.now());
        return this.update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remind(WfCcRemindDTO remindDTO) {
        WfProcessInstance instance = processInstanceService.getById(remindDTO.getInstanceId());
        if (instance == null) {
            throw BusinessException.notFound("流程实例不存在");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(instance.getStartUserId())) {
            throw BusinessException.forbidden("只有发起人可以催读");
        }

        List<WfCcTask> ccTasks;
        if (remindDTO.getCcTaskIds() != null && !remindDTO.getCcTaskIds().isEmpty()) {
            LambdaQueryWrapper<WfCcTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(WfCcTask::getId, remindDTO.getCcTaskIds());
            wrapper.eq(WfCcTask::getInstanceId, remindDTO.getInstanceId());
            wrapper.eq(WfCcTask::getIsRead, 0);
            ccTasks = this.list(wrapper);
        } else {
            LambdaQueryWrapper<WfCcTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WfCcTask::getInstanceId, remindDTO.getInstanceId());
            wrapper.eq(WfCcTask::getIsRead, 0);
            ccTasks = this.list(wrapper);
        }

        if (ccTasks.isEmpty()) {
            log.info("没有需要催读的抄送, instanceId={}", remindDTO.getInstanceId());
            return;
        }

        List<Long> ccTaskIds = ccTasks.stream().map(WfCcTask::getId).collect(Collectors.toList());
        LambdaUpdateWrapper<WfCcTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(WfCcTask::getId, ccTaskIds);
        updateWrapper.setSql("remind_count = remind_count + 1");
        updateWrapper.set(WfCcTask::getLastRemindTime, LocalDateTime.now());
        this.update(updateWrapper);
        log.info("催读抄送成功, instanceId={}, remindCount={}", remindDTO.getInstanceId(), ccTasks.size());

        for (WfCcTask ccTask : ccTasks) {
            sendCcRemindNotifyAsync(ccTask, instance, remindDTO.getRemark());
        }
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

        CcTypeEnum ccTypeEnum = CcTypeEnum.getByCode(ccTask.getCcType());
        if (ccTypeEnum != null) {
            vo.setCcTypeName(ccTypeEnum.getName());
        }

        WfProcessInstance instance = processInstanceService.getById(ccTask.getInstanceId());
        if (instance != null) {
            vo.setTitle(instance.getTitle());
            vo.setStartUserId(instance.getStartUserId());
            vo.setInstanceNo(instance.getInstanceNo());
            vo.setInstanceStatus(instance.getInstanceStatus());

            InstanceStatusEnum statusEnum = InstanceStatusEnum.getByCode(instance.getInstanceStatus());
            if (statusEnum != null) {
                vo.setInstanceStatusName(statusEnum.getName());
            }

            if (instance.getStartUserId() != null) {
                SysUser startUser = sysUserService.getById(instance.getStartUserId());
                if (startUser != null) {
                    vo.setStartUserName(startUser.getRealName());
                }
            }
        }

        if (ccTask.getCcUserId() != null) {
            SysUser ccUser = sysUserService.getById(ccTask.getCcUserId());
            if (ccUser != null) {
                vo.setCcUserName(ccUser.getRealName());
            }
        }

        return vo;
    }

    @Async
    protected void sendCcNotifyAsync(WfCcTask ccTask, WfProcessInstance instance) {
        try {
            NotifySendDTO sendDTO = buildCcNotifySendDTO(ccTask, instance, EventTypeEnum.CC_NOTIFY.getCode(), null);
            notifyService.sendNotify(sendDTO);
            log.info("抄送通知已发送, ccTaskId={}, ccUserId={}", ccTask.getId(), ccTask.getCcUserId());
        } catch (Exception e) {
            log.error("发送抄送通知失败, ccTaskId={}, error={}", ccTask.getId(), e.getMessage(), e);
        }
    }

    @Async
    protected void sendCcRemindNotifyAsync(WfCcTask ccTask, WfProcessInstance instance, String remark) {
        try {
            NotifySendDTO sendDTO = buildCcNotifySendDTO(ccTask, instance, EventTypeEnum.CC_REMIND.getCode(), remark);
            notifyService.sendNotify(sendDTO);
            log.info("抄送催读通知已发送, ccTaskId={}, ccUserId={}", ccTask.getId(), ccTask.getCcUserId());
        } catch (Exception e) {
            log.error("发送抄送催读通知失败, ccTaskId={}, error={}", ccTask.getId(), e.getMessage(), e);
        }
    }

    private NotifySendDTO buildCcNotifySendDTO(WfCcTask ccTask, WfProcessInstance instance,
                                               String eventType, String remark) {
        NotifySendDTO sendDTO = new NotifySendDTO();
        sendDTO.setEventType(eventType);
        sendDTO.setBusinessType("WORKFLOW");
        sendDTO.setInstanceId(ccTask.getInstanceId());
        sendDTO.setReceiverUserId(ccTask.getCcUserId());

        Map<String, Object> params = new HashMap<>();
        params.put("processTitle", instance.getTitle());
        params.put("instanceNo", instance.getInstanceNo());
        params.put("processKey", instance.getProcessKey());
        params.put("startUserId", instance.getStartUserId());
        params.put("nodeId", ccTask.getNodeId());
        params.put("nodeName", ccTask.getNodeName());
        params.put("receiverUserId", ccTask.getCcUserId());
        params.put("instanceId", instance.getId());
        params.put("detailUrl", ccTask.getDetailUrl());
        params.put("ccType", ccTask.getCcType());
        if (StrUtil.isNotBlank(remark)) {
            params.put("remark", remark);
        }

        if (instance.getStartUserId() != null) {
            SysUser startUser = sysUserService.getById(instance.getStartUserId());
            if (startUser != null) {
                params.put("startUserName", startUser.getRealName());
            }
        }

        if (ccTask.getCcUserId() != null) {
            SysUser ccUser = sysUserService.getById(ccTask.getCcUserId());
            if (ccUser != null) {
                params.put("receiverUserName", ccUser.getRealName());
            }
        }

        if (CcTypeEnum.AUTO_PROCESS_END.getCode().equals(ccTask.getCcType())) {
            InstanceStatusEnum statusEnum = InstanceStatusEnum.getByCode(instance.getInstanceStatus());
            if (statusEnum != null) {
                params.put("processResult", statusEnum.getDesc());
            }
        }

        sendDTO.setParams(params);
        return sendDTO;
    }
}
