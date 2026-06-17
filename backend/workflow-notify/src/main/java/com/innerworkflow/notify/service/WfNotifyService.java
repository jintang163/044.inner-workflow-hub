package com.innerworkflow.notify.service;

import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.entity.WfMessageLog;

import java.util.List;

public interface WfNotifyService {

    void sendNotify(NotifySendDTO sendDTO);

    void sendNotifySync(NotifySendDTO sendDTO);

    void processMessageLog(WfMessageLog messageLog);

    List<WfMessageLog> createMessageLogs(NotifySendDTO sendDTO);

    void retryFailedMessages();
}
