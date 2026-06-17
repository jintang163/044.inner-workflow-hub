package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WfCcTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long instanceId;

    private String instanceNo;

    private String processKey;

    private String processName;

    private Long ccUserId;

    private String ccUserName;

    private String nodeId;

    private String nodeName;

    private Integer isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ccTime;

    private String title;

    private Long startUserId;

    private String startUserName;

    private String businessLineName;

    private String categoryName;

    private Integer instanceStatus;

    private String instanceStatusName;
}
