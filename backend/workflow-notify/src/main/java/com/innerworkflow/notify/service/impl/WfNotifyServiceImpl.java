package com.innerworkflow.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.service.WfApprovalTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.IdGenerator;
import com.innerworkflow.common.util.JsonUtils;
import com.innerworkflow.common.util.SpringContextHolder;
import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.entity.WfMessageLog;
import com.innerworkflow.notify.entity.WfMessageTemplate;
import com.innerworkflow.notify.enums.ChannelTypeEnum;
import com.innerworkflow.notify.enums.SendStatusEnum;
import com.innerworkflow.notify.sender.MessageSender;
import com.innerworkflow.notify.service.WfMessageLogService;
import com.innerworkflow.notify.service.WfMessageTemplateService;
import com.innerworkflow.notify.service.WfNotifyService;
import com.innerworkflow.notify.template.MessageTemplateEngine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfNotifyServiceImpl implements WfNotifyService {

    private final WfMessageTemplateService templateService;
    private final WfMessageLogService messageLogService;
    private final MessageTemplateEngine templateEngine;
    private final List<MessageSender> messageSenders;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final Map<String, MessageSender> senderMap = new HashMap<>();

    @Value("${workflow.notify.kafka.topic:workflow_notify}")
    private String notifyTopic;

    @Value("${workflow.notify.max-retry:3}")
    private Integer defaultMaxRetry;

    @PostConstruct
    public void init() {
        for (MessageSender sender : messageSenders) {
            senderMap.put(sender.getChannelType(), sender);
        }
        log.info("消息发送器初始化完成，共{}个渠道: {}", senderMap.size(), senderMap.keySet());
    }

    @Override
    @Async
    public void sendNotify(NotifySendDTO sendDTO) {
        try {
            List<WfMessageLog> messageLogs = createMessageLogs(sendDTO);
            for (WfMessageLog messageLog : messageLogs) {
                try {
                    String messageJson = JsonUtils.toJsonString(messageLog);
                    kafkaTemplate.send(notifyTopic, messageLog.getId().toString(), messageJson);
                    log.debug("通知消息已发送到Kafka, messageNo: {}, channel: {}",
                            messageLog.getMessageNo(), messageLog.getChannelType());
                } catch (Exception e) {
                    log.error("发送通知消息到Kafka失败, messageNo: {}, error: {}",
                            messageLog.getMessageNo(), e.getMessage(), e);
                    handleSendFailureWithUpdate(messageLog, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("发送通知失败, eventType: {}, receiverUserId: {}, error: {}",
                    sendDTO.getEventType(), sendDTO.getReceiverUserId(), e.getMessage(), e);
        }
    }

    @Override
    public void sendNotifySync(NotifySendDTO sendDTO) {
        List<WfMessageLog> messageLogs = createMessageLogs(sendDTO);
        for (WfMessageLog messageLog : messageLogs) {
            processMessageLog(messageLog);
        }
    }

    @Override
    public List<WfMessageLog> createMessageLogs(NotifySendDTO sendDTO) {
        List<WfMessageLog> result = new ArrayList<>();

        WfMessageTemplate template = null;
        List<String> channelTypes = new ArrayList<>();

        if (StrUtil.isNotBlank(sendDTO.getTemplateCode())) {
            template = templateService.getByCode(sendDTO.getTemplateCode());
            if (template == null) {
                throw new BusinessException("消息模板不存在: " + sendDTO.getTemplateCode());
            }
            if (template.getChannelTypes() != null) {
                channelTypes.addAll(template.getChannelTypes());
            }
        } else if (StrUtil.isNotBlank(sendDTO.getEventType())) {
            List<WfMessageTemplate> templates = templateService.getByEventType(
                    sendDTO.getEventType(), null);
            if (!templates.isEmpty()) {
                template = templates.get(0);
                if (template.getChannelTypes() != null) {
                    channelTypes.addAll(template.getChannelTypes());
                }
            }
        }

        if (channelTypes.isEmpty()) {
            log.warn("未配置通知渠道, eventType: {}, templateCode: {}",
                    sendDTO.getEventType(), sendDTO.getTemplateCode());
            return result;
        }

        SysUser receiverUser = resolveReceiverUser(sendDTO.getReceiverUserId());
        SysUser startUser = null;
        SysUser approverUser = null;
        WfProcessInstance instance = null;
        WfApprovalTask task = null;

        if (sendDTO.getInstanceId() != null) {
            try {
                WfProcessInstanceService instanceService =
                        SpringContextHolder.getBean(WfProcessInstanceService.class);
                if (instanceService != null) {
                    instance = instanceService.getById(sendDTO.getInstanceId());
                    if (instance != null && instance.getStartUserId() != null) {
                        startUser = resolveReceiverUser(instance.getStartUserId());
                    }
                }
            } catch (Exception e) {
                log.warn("查询流程实例信息失败, instanceId={}, error={}",
                        sendDTO.getInstanceId(), e.getMessage());
            }
        }

        if (sendDTO.getTaskId() != null) {
            try {
                WfApprovalTaskService taskService =
                        SpringContextHolder.getBean(WfApprovalTaskService.class);
                if (taskService != null) {
                    task = taskService.getById(sendDTO.getTaskId());
                    if (task != null && task.getAssigneeId() != null) {
                        approverUser = resolveReceiverUser(task.getAssigneeId());
                    }
                }
            } catch (Exception e) {
                log.warn("查询审批任务信息失败, taskId={}, error={}",
                        sendDTO.getTaskId(), e.getMessage());
            }
        }

        Map<String, Object> enrichedParams = enrichParams(
                sendDTO.getParams(), instance, task, receiverUser, startUser, approverUser);

        for (String channelType : channelTypes) {
            WfMessageLog messageLog = buildMessageLog(
                    sendDTO, template, channelType, enrichedParams, receiverUser);
            messageLogService.save(messageLog);
            result.add(messageLog);
        }

        return result;
    }

    private SysUser resolveReceiverUser(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            SysUserService userService = SpringContextHolder.getBean(SysUserService.class);
            if (userService != null) {
                return userService.getById(userId);
            }
        } catch (Exception e) {
            log.warn("查询用户信息失败, userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    private Map<String, Object> enrichParams(Map<String, Object> originalParams,
                                             WfProcessInstance instance,
                                             WfApprovalTask task,
                                             SysUser receiverUser,
                                             SysUser startUser,
                                             SysUser approverUser) {
        Map<String, Object> result = new HashMap<>();
        if (originalParams != null) {
            result.putAll(originalParams);
        }

        if (receiverUser != null) {
            result.putIfAbsent("receiverUserName", receiverUser.getRealName());
            result.putIfAbsent("receiverUserNickName", receiverUser.getNickName());
            result.putIfAbsent("receiverUserEmail", receiverUser.getEmail());
            result.putIfAbsent("receiverUserPhone", receiverUser.getPhone());
        }

        if (startUser != null) {
            result.putIfAbsent("startUserName", startUser.getRealName());
            result.putIfAbsent("startUserNickName", startUser.getNickName());
            result.putIfAbsent("startUserDeptId", startUser.getDeptId());
        }

        if (approverUser != null) {
            result.putIfAbsent("approverUserName", approverUser.getRealName());
            result.putIfAbsent("approverUserNickName", approverUser.getNickName());
        }

        if (instance != null) {
            result.putIfAbsent("processTitle", instance.getTitle());
            result.putIfAbsent("instanceNo", instance.getInstanceNo());
            result.putIfAbsent("processKey", instance.getProcessKey());
            result.putIfAbsent("instanceId", instance.getId());
            result.putIfAbsent("startUserId", instance.getStartUserId());
            if (instance.getStartTime() != null) {
                result.putIfAbsent("startTime", instance.getStartTime().toString());
            }
            if (instance.getEndTime() != null) {
                result.putIfAbsent("endTime", instance.getEndTime().toString());
            }
            if (instance.getDuration() != null) {
                result.putIfAbsent("duration", instance.getDuration());
            }
        }

        if (task != null) {
            result.putIfAbsent("taskId", task.getId());
            result.putIfAbsent("nodeId", task.getNodeId());
            result.putIfAbsent("nodeName", task.getNodeName());
            if (task.getAssignTime() != null) {
                result.putIfAbsent("assignTime", task.getAssignTime().toString());
            }
            if (task.getDueTime() != null) {
                result.putIfAbsent("dueTime", task.getDueTime().toString());
            }
        }

        return result;
    }

    private WfMessageLog buildMessageLog(NotifySendDTO sendDTO, WfMessageTemplate template,
                                         String channelType, Map<String, Object> params,
                                         SysUser receiverUser) {
        WfMessageLog messageLog = new WfMessageLog();
        messageLog.setMessageNo(generateMessageNo());
        messageLog.setTemplateId(template != null ? template.getId() : null);
        messageLog.setTemplateCode(template != null ? template.getTemplateCode() : sendDTO.getTemplateCode());
        messageLog.setBusinessType(sendDTO.getBusinessType());
        messageLog.setInstanceId(sendDTO.getInstanceId());
        messageLog.setTaskId(sendDTO.getTaskId());
        messageLog.setChannelType(channelType);
        messageLog.setReceiverUserId(sendDTO.getReceiverUserId());

        String receiverAccount = resolveReceiverAccount(sendDTO.getReceiverAccount(),
                receiverUser, channelType);
        messageLog.setReceiverAccount(receiverAccount);

        messageLog.setMessageParams(params);
        messageLog.setSendStatus(SendStatusEnum.PENDING.getCode());
        messageLog.setRetryCount(0);
        messageLog.setMaxRetry(sendDTO.getMaxRetry() != null ? sendDTO.getMaxRetry() : defaultMaxRetry);
        messageLog.setIsRead(0);
        messageLog.setCreateTime(LocalDateTime.now());
        messageLog.setUpdateTime(LocalDateTime.now());

        String title = "";
        String content = "";
        if (template != null) {
            if (ChannelTypeEnum.EMAIL.getCode().equals(channelType)) {
                title = template.getEmailSubjectTemplate() != null ? template.getEmailSubjectTemplate() : "";
                content = template.getEmailContentTemplate() != null ? template.getEmailContentTemplate() : "";
            } else if (ChannelTypeEnum.SMS.getCode().equals(channelType)) {
                content = template.getSmsTemplateId() != null ? template.getSmsTemplateId() : "";
            } else if (ChannelTypeEnum.DINGTALK.getCode().equals(channelType)) {
                content = template.getDingTemplateId() != null ? template.getDingTemplateId() : "";
                title = template.getTemplateName() != null ? template.getTemplateName() : "";
            } else if (ChannelTypeEnum.WECOM.getCode().equals(channelType)) {
                content = template.getWecomTemplateId() != null ? template.getWecomTemplateId() : "";
                title = template.getTemplateName() != null ? template.getTemplateName() : "";
            } else {
                content = template.getTemplateName() != null ? template.getTemplateName() : "";
            }
        }

        if (params != null && !params.isEmpty()) {
            title = templateEngine.process(title, params);
            content = templateEngine.process(content, params);
        }

        messageLog.setMessageTitle(title);
        messageLog.setMessageContent(content);

        return messageLog;
    }

    private String resolveReceiverAccount(String defaultAccount, SysUser receiverUser, String channelType) {
        if (StrUtil.isNotBlank(defaultAccount)) {
            return defaultAccount;
        }
        if (receiverUser == null) {
            return null;
        }
        if (ChannelTypeEnum.EMAIL.getCode().equals(channelType)) {
            return receiverUser.getEmail();
        } else if (ChannelTypeEnum.SMS.getCode().equals(channelType)) {
            return receiverUser.getPhone();
        } else if (ChannelTypeEnum.DINGTALK.getCode().equals(channelType)) {
            return receiverUser.getDingUserId();
        } else if (ChannelTypeEnum.WECOM.getCode().equals(channelType)) {
            return receiverUser.getWecomUserId();
        }
        return receiverUser.getUsername();
    }

    @Override
    public void processMessageLog(WfMessageLog messageLog) {
        if (messageLog == null) {
            return;
        }

        if (StrUtil.isBlank(messageLog.getReceiverAccount())) {
            log.warn("接收账号为空，跳过发送, messageNo: {}, channelType: {}",
                    messageLog.getMessageNo(), messageLog.getChannelType());
            updateSendStatus(messageLog, SendStatusEnum.FAILED, null, "接收账号为空");
            return;
        }

        MessageSender sender = senderMap.get(messageLog.getChannelType());
        if (sender == null) {
            log.error("未找到消息发送器, channelType: {}", messageLog.getChannelType());
            updateSendStatus(messageLog, SendStatusEnum.FAILED, null, "未找到消息发送器");
            return;
        }

        WfMessageTemplate template = null;
        if (messageLog.getTemplateId() != null) {
            template = templateService.getById(messageLog.getTemplateId());
        }

        try {
            updateSendStatus(messageLog, SendStatusEnum.SENDING, null, null);

            boolean success = sender.send(
                    template,
                    messageLog.getReceiverAccount(),
                    messageLog.getMessageTitle(),
                    messageLog.getMessageContent(),
                    messageLog.getMessageParams()
            );

            if (success) {
                updateSendStatus(messageLog, SendStatusEnum.SUCCESS, null, null);
                log.info("消息发送成功, messageNo: {}, channel: {}, receiver: {}",
                        messageLog.getMessageNo(), messageLog.getChannelType(),
                        maskReceiverAccount(messageLog.getReceiverAccount()));
            } else {
                handleSendFailureWithUpdate(messageLog, "发送失败");
            }
        } catch (Exception e) {
            log.error("消息发送异常, messageNo: {}, error: {}",
                    messageLog.getMessageNo(), e.getMessage(), e);
            handleSendFailureWithUpdate(messageLog, e.getMessage());
        }
    }

    private String maskReceiverAccount(String account) {
        if (StrUtil.isBlank(account)) {
            return "";
        }
        if (account.length() <= 4) {
            return "***";
        }
        return account.substring(0, 2) + "***" + account.substring(account.length() - 2);
    }

    private void handleSendFailureWithUpdate(WfMessageLog messageLog, String failReason) {
        int retryCount = messageLog.getRetryCount() + 1;
        messageLog.setRetryCount(retryCount);

        if (retryCount >= messageLog.getMaxRetry()) {
            updateSendStatus(messageLog, SendStatusEnum.FAILED, null, failReason);
            log.warn("消息发送达到最大重试次数, messageNo: {}, retryCount: {}, reason: {}",
                    messageLog.getMessageNo(), retryCount, failReason);
        } else {
            updateSendStatus(messageLog, SendStatusEnum.PENDING, null, failReason);
            log.info("消息发送失败，等待重试, messageNo: {}, retryCount: {}, reason: {}",
                    messageLog.getMessageNo(), retryCount, failReason);
        }
    }

    private void updateSendStatus(WfMessageLog messageLog, SendStatusEnum status,
                                  String thirdPartyMsgId, String failReason) {
        messageLog.setSendStatus(status.getCode());
        if (SendStatusEnum.SUCCESS.equals(status)) {
            messageLog.setSendTime(LocalDateTime.now());
        }
        if (thirdPartyMsgId != null) {
            messageLog.setThirdPartyMsgId(thirdPartyMsgId);
        }
        if (failReason != null) {
            messageLog.setFailReason(failReason);
        }
        messageLog.setUpdateTime(LocalDateTime.now());
        try {
            messageLogService.updateById(messageLog);
        } catch (Exception e) {
            log.error("更新消息日志状态失败, messageNo: {}, error: {}",
                    messageLog.getMessageNo(), e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void retryFailedMessages() {
        List<WfMessageLog> pendingList = messageLogService.getPendingRetryList(100);
        log.info("开始重试失败消息，共{}条", pendingList.size());
        for (WfMessageLog messageLog : pendingList) {
            try {
                processMessageLog(messageLog);
            } catch (Exception e) {
                log.error("重试消息异常, messageId: {}, error: {}",
                        messageLog.getId(), e.getMessage(), e);
            }
        }
    }

    private String generateMessageNo() {
        return "MSG" + IdGenerator.nextIdStr();
    }
}
