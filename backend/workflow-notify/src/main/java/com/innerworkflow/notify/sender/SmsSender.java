package com.innerworkflow.notify.sender;

import com.innerworkflow.notify.entity.WfMessageLog;
import com.innerworkflow.notify.entity.WfMessageTemplate;
import com.innerworkflow.notify.enums.ChannelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SmsSender implements MessageSender {

    @Override
    public String getChannelType() {
        return ChannelTypeEnum.SMS.getCode();
    }

    @Override
    public boolean send(WfMessageTemplate template, String receiver, String title, String content, Map<String, Object> params) {
        try {
            log.info("发送短信 - 手机号: {}, 模板ID: {}", receiver, template.getSmsTemplateId());
            log.debug("短信内容: {}", content);
            return true;
        } catch (Exception e) {
            log.error("短信发送失败 - 手机号: {}, 原因: {}", receiver, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void updateLogAfterSend(WfMessageLog logEntity, boolean success, String thirdPartyMsgId, String failReason) {
        if (success) {
            logEntity.setThirdPartyMsgId(thirdPartyMsgId);
        } else {
            logEntity.setFailReason(failReason);
        }
    }
}
