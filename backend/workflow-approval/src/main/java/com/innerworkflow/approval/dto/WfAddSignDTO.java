package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfAddSignDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @NotNull(message = "加签人ID列表不能为空")
    private List<Long> targetUserIds;

    private Integer signType;

    private String actionRemark;
}
