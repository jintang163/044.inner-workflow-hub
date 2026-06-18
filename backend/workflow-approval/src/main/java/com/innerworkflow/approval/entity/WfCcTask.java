package com.innerworkflow.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_cc_task")
public class WfCcTask extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long instanceId;

    private String processKey;

    private Long ccUserId;

    private String nodeId;

    private String nodeName;

    private Integer ccType;

    private Integer isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ccTime;

    private Integer remindCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRemindTime;

    private String detailUrl;
}
