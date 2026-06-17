package com.innerworkflow.approval.service;

import com.innerworkflow.approval.entity.WfTaskRelation;

import java.util.List;

public interface WfTaskRelationService {

    List<WfTaskRelation> listByParentTaskId(Long parentTaskId);

    List<WfTaskRelation> listByChildTaskId(Long childTaskId);

    boolean save(WfTaskRelation relation);

    boolean saveBatch(List<WfTaskRelation> list);
}
