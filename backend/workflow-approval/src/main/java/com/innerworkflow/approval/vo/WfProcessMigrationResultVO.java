package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class WfProcessMigrationResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String migrationNo;

    private Long recordId;

    private String processKey;

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<MigrationDetailItem> details = new ArrayList<>();

    @Data
    public static class MigrationDetailItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long detailId;
        private Long instanceId;
        private String instanceNo;
        private String title;
        private String startUserName;
        private Integer migrationResult;
        private String migrationResultName;
        private String skipReason;
        private String errorMessage;
        private List<String> sourceCurrentNodeIds;
        private List<String> targetCurrentNodeIds;
        private CompatibilityCheckVO compatibilityCheck;
    }
}
