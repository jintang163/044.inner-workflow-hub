package com.innerworkflow.notify.sender;

import com.innerworkflow.notify.entity.WfMessageLog;
import com.innerworkflow.notify.entity.WfMessageTemplate;
import com.innerworkflow.notify.enums.ChannelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class DingTalkSender implements MessageSender {

    @Override
    public String getChannelType() {
        return ChannelTypeEnum.DINGTALK.getCode();
    }

    @Override
    public boolean send(WfMessageTemplate template, String receiver, String title, String content, Map<String, Object> params) {
        try {
            log.info("发送钉钉消息 - 接收人: {}, 标题: {}, 模板ID: {}", receiver, title, template.getDingTemplateId());
            log.debug("钉钉消息内容: {}", content);
            return true;
        } catch (Exception e) {
            log.error("钉钉消息发送失败 - 接收人: {}, 原因: {}", receiver, e.getMessage(), e);
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
