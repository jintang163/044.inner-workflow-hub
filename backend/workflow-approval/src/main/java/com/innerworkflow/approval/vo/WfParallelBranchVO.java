package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WfParallelBranchVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String branchId;

    private String branchName;

    private String status;

    private String statusName;

    private Long currentApproverId;

    private String currentApproverName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private List<String> completedNodeIds;

    private List<String> pendingNodeIds;

    private Boolean isRejected;
}
