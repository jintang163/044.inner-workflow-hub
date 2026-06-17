package com.innerworkflow.approval.service;

import com.innerworkflow.approval.entity.WfTimeoutRemind;

import java.util.List;

public interface WfTimeoutRemindService {

    List<WfTimeoutRemind> listByTaskId(Long taskId);

    int countByTaskId(Long taskId);

    boolean save(WfTimeoutRemind timeoutRemind);

    List<WfTimeoutRemind> listByInstanceId(Long instanceId);
}
