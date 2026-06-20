package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class WfTaskRemindDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    private String remark;
}
