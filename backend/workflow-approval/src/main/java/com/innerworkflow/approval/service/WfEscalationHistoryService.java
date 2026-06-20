package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.entity.WfEscalationHistory;
import com.innerworkflow.common.dto.PageQuery;

import java.util.List;

public interface WfEscalationHistoryService {

    List<WfEscalationHistory> listByInstanceId(Long instanceId);

    List<WfEscalationHistory> listByTaskId(Long taskId);

    IPage<WfEscalationHistory> pageByInstanceId(Long instanceId, PageQuery query);

    IPage<WfEscalationHistory> pageByTaskId(Long taskId, PageQuery query);

    boolean save(WfEscalationHistory history);
}
