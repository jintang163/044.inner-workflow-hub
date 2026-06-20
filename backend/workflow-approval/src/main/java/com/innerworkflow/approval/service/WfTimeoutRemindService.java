package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfBatchRemindDTO;
import com.innerworkflow.approval.entity.WfTimeoutRemind;
import com.innerworkflow.approval.vo.WfBatchRemindResultVO;
import com.innerworkflow.common.dto.PageQuery;

import java.time.LocalDateTime;
import java.util.List;

public interface WfTimeoutRemindService {

    List<WfTimeoutRemind> listByTaskId(Long taskId);

    int countByTaskId(Long taskId);

    boolean save(WfTimeoutRemind timeoutRemind);

    List<WfTimeoutRemind> listByInstanceId(Long instanceId);

    boolean manualRemind(Long taskId, String remark);

    IPage<WfTimeoutRemind> pageByTaskId(Long taskId, PageQuery query);

    IPage<WfTimeoutRemind> pageByInstanceId(Long instanceId, PageQuery query);

    int countManualRemindByTaskId(Long taskId);

    WfBatchRemindResultVO batchRemind(WfBatchRemindDTO dto);

    LocalDateTime getLastRemindTime(Long taskId);

    boolean checkRemindInterval(Long taskId, int intervalMinutes);
}
