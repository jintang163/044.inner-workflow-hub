package com.innerworkflow.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "wf_process_migration_detail", autoResultMap = true)
public class WfProcessMigrationDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private String migrationNo;

    private Long instanceId;

    private String instanceNo;

    private String title;

    private Long startUserId;

    private String startUserName;

    private String sourceFlowableProcInstId;

    private String sourceFlowableProcDefId;

    private Long sourceVersionId;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> sourceCurrentNodeIds;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<Long> sourceCurrentApproverIds;

    private String targetFlowableProcInstId;

    private String targetFlowableProcDefId;

    private Long targetVersionId;

    private Long targetProcessVersionId;

    private Integer migrationResult;

    private String skipReason;

    private String errorMessage;

    private String compatibilityCheck;

    private String backupInstanceData;

    private String backupTasksData;

    private String backupVariablesData;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime migrateTime;

    private Long tenantId;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private Integer isDeleted;
}
