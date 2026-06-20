package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfBatchRemindDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "任务ID不能为空")
    private List<Long> taskIds;

    private String remark;
}
