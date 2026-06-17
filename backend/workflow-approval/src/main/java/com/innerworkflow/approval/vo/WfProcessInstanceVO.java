package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WfProcessInstanceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String instanceNo;

    private Long processDefinitionId;

    private String processKey;

    private String processName;

    private Long processVersionId;

    private String flowableProcessInstId;

    private Long businessLineId;

    private String businessLineName;

    private Long categoryId;

    private String categoryName;

    private String title;

    private Long formId;

    private Integer formVersion;

    private Object formData;

    private Integer instanceStatus;

    private String instanceStatusName;

    private Long startUserId;

    private String startUserName;

    private Long startDeptId;

    private String startDeptName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Long duration;

    private List<String> currentNodeIds;

    private List<String> currentNodeNames;

    private List<Long> currentApproverIds;

    private List<String> currentApproverNames;

    private Integer priority;

    private String priorityName;
}
