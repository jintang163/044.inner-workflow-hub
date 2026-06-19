package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfDoneTaskQueryDTO;
import com.innerworkflow.approval.dto.WfTodoTaskQueryDTO;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.vo.WfApprovalTaskVO;

import java.util.List;

public interface WfApprovalTaskService {

    IPage<WfApprovalTaskVO> pageTodo(WfTodoTaskQueryDTO queryDTO);

    IPage<WfApprovalTaskVO> pageDone(WfDoneTaskQueryDTO queryDTO);

    WfApprovalTask getById(Long id);

    WfApprovalTask getByFlowableTaskId(String flowableTaskId);

    List<WfApprovalTask> listByInstanceId(Long instanceId);

    List<WfApprovalTask> listTodoByInstanceId(Long instanceId);

    boolean save(WfApprovalTask task);

    boolean updateById(WfApprovalTask task);

    boolean updateByFlowableTaskId(String flowableTaskId, WfApprovalTask task);

    long countTodoByUserId(Long userId);

    List<WfApprovalTask> listTodoByUserId(Long userId);

    WfApprovalTask getByInstanceIdAndNodeId(Long instanceId, String nodeId);
}
