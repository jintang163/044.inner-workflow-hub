package com.innerworkflow.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wf_escalation_history")
public class WfEscalationHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long instanceId;

    private Long taskId;

    private Long ruleId;

    private Integer escalateLevel;

    private Integer escalateType;

    private String escalateTarget;

    private Integer escalateAction;

    private Long fromUserId;

    private String toUserIds;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggerTime;

    private Integer triggerType;

    private String remark;

    private Long tenantId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
