package com.innerworkflow.notify.sender;

import com.innerworkflow.notify.entity.WfMessageLog;
import com.innerworkflow.notify.entity.WfMessageTemplate;

import java.util.Map;

public interface MessageSender {

    String getChannelType();

    boolean send(WfMessageTemplate template, String receiver, String title, String content, Map<String, Object> params);

    default void updateLogAfterSend(WfMessageLog log, boolean success, String thirdPartyMsgId, String failReason) {
    }
}
