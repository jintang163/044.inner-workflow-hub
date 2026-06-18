package com.innerworkflow.approval.service;

import com.innerworkflow.approval.entity.WfProcessInstanceRelation;
import com.innerworkflow.approval.vo.WfProcessInstanceRelationVO;

import java.util.List;

public interface WfProcessInstanceRelationService {

    boolean save(WfProcessInstanceRelation relation);

    WfProcessInstanceRelation getByChildInstanceId(Long childInstanceId);

    List<WfProcessInstanceRelationVO> listByParentInstanceId(Long parentInstanceId);

    List<WfProcessInstanceRelationVO> listByParentInstanceIdAndNodeId(Long parentInstanceId, String nodeId);

    boolean updateChildInstanceIdByFlowableInstId(String childFlowableInstId, Long childInstanceId);
}
