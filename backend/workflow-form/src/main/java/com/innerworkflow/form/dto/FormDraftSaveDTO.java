package com.innerworkflow.form.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class FormDraftSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String draftNo;

    @NotNull(message = "流程定义ID不能为空")
    private Long processDefinitionId;

    @NotNull(message = "流程标识不能为空")
    private String processKey;

    private Long processVersionId;

    private String processName;

    private String title;

    @NotNull(message = "表单ID不能为空")
    private Long formId;

    @NotNull(message = "表单版本不能为空")
    private Integer formVersion;

    private Long formDefinitionId;

    private Object formData;

    private Integer draftStatus;

    private List<Long> attachmentIds;

    private List<Long> ccUserIds;
}
