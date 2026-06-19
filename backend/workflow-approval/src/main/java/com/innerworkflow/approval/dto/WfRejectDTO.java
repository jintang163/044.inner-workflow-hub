package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfRejectDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    private Long instanceId;

    private Integer version;

    private String targetNodeId;

    private String targetNodeName;

    @NotBlank(message = "驳回理由不能为空")
    @Size(min = 5, max = 500, message = "驳回理由长度应在5-500字之间")
    private String actionRemark;

    private String signatureUrl;

    private List<Long> attachmentIds;

    private Boolean resetFormData;
}
