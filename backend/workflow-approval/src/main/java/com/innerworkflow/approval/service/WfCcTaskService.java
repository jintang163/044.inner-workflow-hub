package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfCcAddDTO;
import com.innerworkflow.approval.dto.WfCcRemindDTO;
import com.innerworkflow.approval.dto.WfCcTaskQueryDTO;
import com.innerworkflow.approval.entity.WfCcTask;
import com.innerworkflow.approval.vo.WfCcTaskVO;

import java.util.List;

public interface WfCcTaskService {

    IPage<WfCcTaskVO> page(WfCcTaskQueryDTO queryDTO);

    WfCcTask getById(Long id);

    List<WfCcTask> listByInstanceId(Long instanceId);

    List<WfCcTaskVO> listVOByInstanceId(Long instanceId);

    boolean save(WfCcTask ccTask);

    boolean saveBatch(List<WfCcTask> list);

    void addCc(WfCcAddDTO addDTO);

    boolean markRead(Long id);

    boolean markReadBatch(List<Long> ids);

    void remind(WfCcRemindDTO remindDTO);

    long countUnreadByUserId(Long userId);
}
