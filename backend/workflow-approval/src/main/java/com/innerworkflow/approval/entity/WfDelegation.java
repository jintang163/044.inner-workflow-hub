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
@TableName("wf_delegation")
public class WfDelegation extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long delegatorId;

    private String delegatorName;

    private Long delegateeId;

    private String delegateeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String delegationReason;

    private Integer delegationStatus;

    private String processKeys;

    private String remark;
}
