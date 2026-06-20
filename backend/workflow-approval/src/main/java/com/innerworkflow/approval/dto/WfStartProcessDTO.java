package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class WfStartProcessDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "流程标识不能为空")
    private String processKey;

    @NotBlank(message = "审批标题不能为空")
    private String title;

    @NotNull(message = "表单数据不能为空")
    private Map<String, Object> formData;

    private List<Long> ccUserIds;

    private List<Long> attachmentIds;

    private Integer priority;

    private String remark;

    private Long draftId;
}
