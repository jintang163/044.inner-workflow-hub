package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfInstanceMigrationQueryDTO;
import com.innerworkflow.approval.dto.WfProcessMigrationDTO;
import com.innerworkflow.approval.vo.CompatibilityCheckVO;
import com.innerworkflow.approval.vo.WfMigrateInstanceVO;
import com.innerworkflow.approval.vo.WfProcessMigrationRecordVO;
import com.innerworkflow.approval.vo.WfProcessMigrationResultVO;
import com.innerworkflow.common.dto.PageQuery;

import java.util.List;

public interface WfProcessMigrationService {

    IPage<WfMigrateInstanceVO> pageMigratableInstances(WfInstanceMigrationQueryDTO queryDTO);

    List<WfMigrateInstanceVO> listMigratableInstances(WfInstanceMigrationQueryDTO queryDTO);

    CompatibilityCheckVO checkCompatibility(Long instanceId, Long targetVersionId);

    WfProcessMigrationResultVO batchMigrate(WfProcessMigrationDTO dto);

    WfProcessMigrationResultVO getMigrationResult(Long recordId);

    IPage<WfProcessMigrationRecordVO> pageMigrationRecords(Long processDefinitionId, PageQuery query);

    byte[] downloadBackup(Long recordId);
}
