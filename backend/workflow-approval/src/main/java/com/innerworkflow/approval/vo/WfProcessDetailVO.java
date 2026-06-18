package com.innerworkflow.approval.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfProcessDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private WfProcessInstanceVO instance;

    private List<WfApprovalTaskVO> currentTasks;

    private List<WfApprovalHistoryVO> historyList;

    private String bpmnXml;

    private List<String> highLightedNodeIds;

    private List<String> highLightedFlowIds;

    private Object formData;

    private List<WfAttachmentVO> attachments;

    private List<WfCcTaskVO> ccTaskList;

    private Long ccUnreadCount;

    private List<WfParallelProgressVO> parallelProgressList;

    private Boolean canWithdraw;

    private Boolean canApprove;

    private Boolean canReject;

    private Boolean canTransfer;

    private Boolean canAddSign;

    private Boolean canDelegate;
}
