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
@TableName(value = "wf_process_migration_record", autoResultMap = true)
public class WfProcessMigrationRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String migrationNo;

    private String processKey;

    private Long processDefinitionId;

    private Long sourceVersionId;

    private Integer sourceVersion;

    private Long targetVersionId;

    private Integer targetVersion;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private Integer skipCount;

    private Integer migrationStatus;

    private String backupSnapshot;

    private String errorMessage;

    private String remark;

    private Long tenantId;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private Integer isDeleted;
}
