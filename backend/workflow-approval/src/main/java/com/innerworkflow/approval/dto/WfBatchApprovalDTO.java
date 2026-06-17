package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfBatchApprovalDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "任务ID列表不能为空")
    private List<String> taskIds;

    @NotNull(message = "审批动作不能为空")
    private Integer action;

    private String actionRemark;
}
