package com.innerworkflow.form.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FormDraftVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String draftNo;

    private Long processDefinitionId;

    private String processKey;

    private Long processVersionId;

    private String processName;

    private String title;

    private Long formId;

    private Integer formVersion;

    private Long formDefinitionId;

    private Object formData;

    private Integer draftStatus;

    private String draftStatusName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAutoSaveTime;

    private Integer autoSaveCount;

    private List<Long> attachmentIds;

    private List<Long> ccUserIds;

    private Long creatorId;

    private String creatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
