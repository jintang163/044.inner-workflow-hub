package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WfProcessMigrationRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private String migrationStatusName;

    private String remark;

    private Long createBy;

    private String createByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
