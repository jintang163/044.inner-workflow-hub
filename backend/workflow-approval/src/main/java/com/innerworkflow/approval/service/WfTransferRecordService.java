package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.common.dto.PageQuery;
import com.innerworkflow.approval.entity.WfTransferRecord;
import com.innerworkflow.approval.vo.WfTransferRecordVO;

import java.util.List;

public interface WfTransferRecordService {

    IPage<WfTransferRecordVO> page(PageQuery queryDTO, Long userId, Integer transferType);

    boolean save(WfTransferRecord record);

    boolean saveBatch(List<WfTransferRecord> list);

    WfTransferRecord getById(Long id);

    List<WfTransferRecord> listByTaskId(Long taskId);

    List<WfTransferRecordVO> listVOByInstanceId(Long instanceId);

    void createTransferRecord(Long instanceId, Long taskId, Integer transferType,
                              Long sourceUserId, String sourceUserName,
                              Long targetUserId, String targetUserName,
                              String transferReason, Long delegationId);
}
