package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfProcessMigrationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "目标版本ID不能为空")
    private Long targetVersionId;

    @NotNull(message = "流程定义ID不能为空")
    private Long processDefinitionId;

    @NotEmpty(message = "流程实例ID不能为空")
    private List<Long> instanceIds;

    private String remark;

    private Boolean forceMigrate = false;
}
