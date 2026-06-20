package com.innerworkflow.approval.vo;

import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfMigrateInstanceVO extends WfProcessInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer currentVersion;

    private Integer latestVersion;

    private Long latestVersionId;

    private String versionRemark;

    private Integer versionGap;

    private Boolean canMigrate;

    private String migrateTip;

    private List<WfProcessVersion> availableVersions;
}
