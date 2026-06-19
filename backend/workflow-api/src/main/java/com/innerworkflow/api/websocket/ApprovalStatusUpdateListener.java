package com.innerworkflow.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innerworkflow.common.event.ApprovalStatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalStatusUpdateListener {

    private final ApprovalWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @EventListener
    public void handleApprovalStatusUpdate(ApprovalStatusUpdateEvent event) {
        try {
            ApprovalStatusUpdateEvent.EventPayload payload = event.getPayload();
            if (payload == null || payload.getInstanceNo() == null) {
                return;
            }

            ApprovalStatusUpdateVO vo = ApprovalStatusUpdateVO.builder()
                    .type("STATUS_UPDATE")
                    .instanceId(payload.getInstanceId())
                    .instanceNo(payload.getInstanceNo())
                    .instanceStatus(payload.getInstanceStatus())
                    .instanceStatusName(payload.getInstanceStatusName())
                    .actionType(payload.getActionType())
                    .actionTypeName(payload.getActionTypeName())
                    .operatorId(payload.getOperatorId())
                    .operatorName(payload.getOperatorName())
                    .version(payload.getVersion())
                    .timestamp(payload.getTimestamp() != null
                            ? payload.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : null)
                    .build();

            webSocketHandler.pushStatusUpdate(payload.getInstanceNo(), vo);
        } catch (Exception e) {
            log.warn("处理审批状态更新事件失败, instanceNo={}, error={}",
                    payload != null ? payload.getInstanceNo() : null, e.getMessage());
        }
    }
}
