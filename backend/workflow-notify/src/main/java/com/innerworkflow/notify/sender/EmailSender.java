package com.innerworkflow.notify.sender;

import com.innerworkflow.notify.entity.WfMessageLog;
import com.innerworkflow.notify.entity.WfMessageTemplate;
import com.innerworkflow.notify.enums.ChannelTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender implements MessageSender {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public String getChannelType() {
        return ChannelTypeEnum.EMAIL.getCode();
    }

    @Override
    public boolean send(WfMessageTemplate template, String receiver, String title, String content, Map<String, Object> params) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getUsername());
            message.setTo(receiver);
            message.setSubject(title);
            message.setText(content);
            javaMailSender.send(message);
            log.info("邮件发送成功 - 接收人: {}, 标题: {}", receiver, title);
            return true;
        } catch (Exception e) {
            log.error("邮件发送失败 - 接收人: {}, 原因: {}", receiver, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void updateLogAfterSend(WfMessageLog logEntity, boolean success, String thirdPartyMsgId, String failReason) {
        if (!success) {
            logEntity.setFailReason(failReason);
        }
    }
}
