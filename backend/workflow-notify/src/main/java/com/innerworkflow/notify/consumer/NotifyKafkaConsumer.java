package com.innerworkflow.notify.consumer;

import com.innerworkflow.common.util.JsonUtils;
import com.innerworkflow.notify.entity.WfMessageLog;
import com.innerworkflow.notify.service.WfNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyKafkaConsumer {

    private final WfNotifyService notifyService;

    @KafkaListener(topics = "${workflow.notify.kafka.topic:workflow_notify}",
            groupId = "${workflow.notify.kafka.group:notify-group}")
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            String messageJson = record.value();
            log.debug("收到通知消息, key: {}, topic: {}", record.key(), record.topic());

            WfMessageLog messageLog = JsonUtils.parseObject(messageJson, WfMessageLog.class);
            if (messageLog == null || messageLog.getId() == null) {
                log.warn("通知消息格式错误, message: {}", messageJson);
                return;
            }

            notifyService.processMessageLog(messageLog);
        } catch (Exception e) {
            log.error("消费通知消息失败, key: {}, error: {}", record.key(), e.getMessage(), e);
        }
    }
}
