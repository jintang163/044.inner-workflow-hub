package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WfProcessInstanceRelationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentInstanceId;

    private Long childInstanceId;

    private String childProcessKey;

    private String childProcessName;

    private String childInstanceNo;

    private String childInstanceTitle;

    private Integer childInstanceStatus;

    private String childInstanceStatusName;

    private Long childStartUserId;

    private String childStartUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime childStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime childEndTime;

    private String callActivityNodeId;

    private String callActivityNodeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
