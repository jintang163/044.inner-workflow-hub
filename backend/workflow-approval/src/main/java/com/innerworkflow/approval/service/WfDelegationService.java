package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfDelegationQueryDTO;
import com.innerworkflow.approval.dto.WfDelegationSaveDTO;
import com.innerworkflow.approval.entity.WfDelegation;
import com.innerworkflow.approval.vo.WfDelegationVO;

import java.util.List;

public interface WfDelegationService {

    IPage<WfDelegationVO> page(WfDelegationQueryDTO queryDTO);

    WfDelegationVO getById(Long id);

    void saveDelegation(WfDelegationSaveDTO dto);

    void updateDelegation(WfDelegationSaveDTO dto);

    void revokeDelegation(Long id);

    List<WfDelegation> listActiveDelegationsByDelegatorId(Long delegatorId);

    WfDelegation getActiveDelegation(Long delegatorId, String processKey);

    void processDelegationStatus();

    List<WfDelegation> listPendingStartDelegations();

    List<WfDelegation> listPendingEndDelegations();

    boolean updateStatus(Long id, Integer status);

    boolean save(WfDelegation delegation);

    boolean updateById(WfDelegation delegation);
}
