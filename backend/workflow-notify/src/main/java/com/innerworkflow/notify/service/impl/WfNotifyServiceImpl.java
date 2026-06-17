package com.innerworkflow.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.IdGenerator;
import com.innerworkflow.common.util.JsonUtils;
import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.entity.WfMessageLog;
import com.innerworkflow.notify.entity.WfMessageTemplate;
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
    public void sendNotify(NotifySendDTO sendDTO) {
        List<WfMessageLog> messageLogs = createMessageLogs(sendDTO);
        for (WfMessageLog messageLog : messageLogs) {
            try {
                String messageJson = JsonUtils.toJsonString(messageLog);
                kafkaTemplate.send(notifyTopic, messageLog.getId().toString(), messageJson);
                log.debug("通知消息已发送到Kafka, messageNo: {}", messageLog.getMessageNo());
            } catch (Exception e) {
                log.error("发送通知消息到Kafka失败, messageNo: {}, error: {}",
                        messageLog.getMessageNo(), e.getMessage(), e);
            }
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

        for (String channelType : channelTypes) {
            WfMessageLog messageLog = buildMessageLog(sendDTO, template, channelType);
            messageLogService.save(messageLog);
            result.add(messageLog);
        }

        return result;
    }

    private WfMessageLog buildMessageLog(NotifySendDTO sendDTO, WfMessageTemplate template, String channelType) {
        WfMessageLog messageLog = new WfMessageLog();
        messageLog.setMessageNo(generateMessageNo());
        messageLog.setTemplateId(template != null ? template.getId() : null);
        messageLog.setTemplateCode(template != null ? template.getTemplateCode() : sendDTO.getTemplateCode());
        messageLog.setBusinessType(sendDTO.getBusinessType());
        messageLog.setInstanceId(sendDTO.getInstanceId());
        messageLog.setTaskId(sendDTO.getTaskId());
        messageLog.setChannelType(channelType);
        messageLog.setReceiverUserId(sendDTO.getReceiverUserId());
        messageLog.setReceiverAccount(sendDTO.getReceiverAccount());
        messageLog.setMessageParams(sendDTO.getParams());
        messageLog.setSendStatus(SendStatusEnum.PENDING.getCode());
        messageLog.setRetryCount(0);
        messageLog.setMaxRetry(sendDTO.getMaxRetry() != null ? sendDTO.getMaxRetry() : defaultMaxRetry);
        messageLog.setIsRead(0);
        messageLog.setCreateTime(LocalDateTime.now());
        messageLog.setUpdateTime(LocalDateTime.now());

        String title = "";
        String content = "";
        if (template != null) {
            if ("EMAIL".equals(channelType)) {
                title = template.getEmailSubjectTemplate() != null ? template.getEmailSubjectTemplate() : "";
                content = template.getEmailContentTemplate() != null ? template.getEmailContentTemplate() : "";
            } else if ("SMS".equals(channelType)) {
                content = template.getSmsTemplateId() != null ? template.getSmsTemplateId() : "";
            } else {
                content = template.getTemplateName();
            }
        }

        if (sendDTO.getParams() != null && !sendDTO.getParams().isEmpty()) {
            title = templateEngine.process(title, sendDTO.getParams());
            content = templateEngine.process(content, sendDTO.getParams());
        }

        messageLog.setMessageTitle(title);
        messageLog.setMessageContent(content);

        return messageLog;
    }

    @Override
    public void processMessageLog(WfMessageLog messageLog) {
        if (messageLog == null) {
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
                log.info("消息发送成功, messageNo: {}, channel: {}",
                        messageLog.getMessageNo(), messageLog.getChannelType());
            } else {
                handleSendFailure(messageLog, "发送失败");
            }
        } catch (Exception e) {
            log.error("消息发送异常, messageNo: {}, error: {}",
                    messageLog.getMessageNo(), e.getMessage(), e);
            handleSendFailure(messageLog, e.getMessage());
        }
    }

    private void handleSendFailure(WfMessageLog messageLog, String failReason) {
        int retryCount = messageLog.getRetryCount() + 1;
        messageLog.setRetryCount(retryCount);

        if (retryCount >= messageLog.getMaxRetry()) {
            updateSendStatus(messageLog, SendStatusEnum.FAILED, null, failReason);
            log.warn("消息发送达到最大重试次数, messageNo: {}, retryCount: {}",
                    messageLog.getMessageNo(), retryCount);
        } else {
            updateSendStatus(messageLog, SendStatusEnum.PENDING, null, failReason);
            log.info("消息发送失败，等待重试, messageNo: {}, retryCount: {}",
                    messageLog.getMessageNo(), retryCount);
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
        messageLogService.updateById(messageLog);
    }

    @Override
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
