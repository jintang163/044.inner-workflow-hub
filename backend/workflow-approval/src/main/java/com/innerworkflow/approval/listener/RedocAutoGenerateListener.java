package com.innerworkflow.approval.listener;

import com.innerworkflow.approval.service.WfRedocTemplateService;
import com.innerworkflow.common.enums.InstanceStatusEnum;
import com.innerworkflow.common.event.ApprovalStatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedocAutoGenerateListener {

    private final WfRedocTemplateService redocTemplateService;

    @Async
    @EventListener
    public void onApprovalStatusUpdate(ApprovalStatusUpdateEvent event) {
        try {
            ApprovalStatusUpdateEvent.EventPayload payload = event.getPayload();
            if (payload == null || payload.getInstanceNo() == null) return;

            Integer status = payload.getInstanceStatus();
            if (status == null) return;

            if (InstanceStatusEnum.APPROVED.getCode().equals(status)) {
                log.info("检测到审批完成，触发红头文件自动生成: instanceNo={}", payload.getInstanceNo());
                redocTemplateService.autoGenerateForInstance(payload.getInstanceNo());
            }
        } catch (Exception e) {
            log.error("自动生成红头文件监听处理异常: {}", e.getMessage(), e);
        }
    }
}
