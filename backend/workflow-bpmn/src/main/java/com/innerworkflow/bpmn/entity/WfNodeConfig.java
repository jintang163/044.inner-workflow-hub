package com.innerworkflow.bpmn.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "wf_node_config", autoResultMap = true)
public class WfNodeConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long processVersionId;

    private String processKey;

    private String nodeId;

    private String nodeName;

    private String nodeType;

    private Integer approveType;

    private Integer multiInstance;

    private Integer assigneeType;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<Long> assigneeValue;

    private String assigneeScript;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Map<String, String> formPermission;

    private Integer timeoutStrategy;

    private Integer timeoutHours;

    private Integer timeoutEscalateLevels;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Map<String, Object> notifyConfig;

    private Integer emptyAssigneeStrategy;

    private Integer refuseStrategy;

    private String refuseTargetNodeId;

    private Integer parallelRejectStrategy;

    private Integer canAddSign;

    private Integer canTransfer;

    private Integer canDelegate;

    private Integer needSignature;

    private Integer needComment;

    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
