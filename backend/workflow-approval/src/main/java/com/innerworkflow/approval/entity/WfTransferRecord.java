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
@TableName("wf_transfer_record")
public class WfTransferRecord extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long instanceId;

    private String instanceNo;

    private Long taskId;

    private String flowableTaskId;

    private String nodeId;

    private String nodeName;

    private Integer transferType;

    private Long sourceUserId;

    private String sourceUserName;

    private Long targetUserId;

    private String targetUserName;

    private String transferReason;

    private Long delegationId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
