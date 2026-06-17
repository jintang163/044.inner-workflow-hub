package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfCcTaskQueryDTO;
import com.innerworkflow.approval.entity.WfCcTask;
import com.innerworkflow.approval.vo.WfCcTaskVO;

import java.util.List;

public interface WfCcTaskService {

    IPage<WfCcTaskVO> page(WfCcTaskQueryDTO queryDTO);

    WfCcTask getById(Long id);

    List<WfCcTask> listByInstanceId(Long instanceId);

    boolean save(WfCcTask ccTask);

    boolean saveBatch(List<WfCcTask> list);

    boolean markRead(Long id);

    long countUnreadByUserId(Long userId);
}
