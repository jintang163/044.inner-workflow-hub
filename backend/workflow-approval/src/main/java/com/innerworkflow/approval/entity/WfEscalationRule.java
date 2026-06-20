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
@TableName("wf_escalation_rule")
public class WfEscalationRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String ruleName;

    private String ruleCode;

    private String processKey;

    private String nodeId;

    private Integer escalateLevel;

    private Integer timeoutHours;

    private Integer escalateType;

    private String escalateTarget;

    private Integer escalateAction;

    private Integer enabled;

    private Integer sortOrder;

    private Long tenantId;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
