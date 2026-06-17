package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class WfRejectDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    private String targetNodeId;

    private String actionRemark;
}
