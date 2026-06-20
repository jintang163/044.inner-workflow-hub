package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfInstanceMigrationQueryDTO;
import com.innerworkflow.approval.dto.WfProcessMigrationDTO;
import com.innerworkflow.approval.service.WfProcessMigrationService;
import com.innerworkflow.approval.vo.CompatibilityCheckVO;
import com.innerworkflow.approval.vo.WfMigrateInstanceVO;
import com.innerworkflow.approval.vo.WfProcessMigrationRecordVO;
import com.innerworkflow.approval.vo.WfProcessMigrationResultVO;
import com.innerworkflow.common.dto.PageQuery;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "流程实例迁移", description = "历史流程实例迁移")
@RestController
@RequestMapping("/approval/migration")
@RequiredArgsConstructor
public class WfProcessMigrationController {

    private final WfProcessMigrationService migrationService;

    @Operation(summary = "分页查询可迁移的流程实例")
    @GetMapping("/instances")
    public R<IPage<WfMigrateInstanceVO>> pageMigratableInstances(WfInstanceMigrationQueryDTO queryDTO) {
        return R.success(migrationService.pageMigratableInstances(queryDTO));
    }

    @Operation(summary = "获取可迁移的流程实例列表(批量选择用)")
    @GetMapping("/instances/list")
    public R<List<WfMigrateInstanceVO>> listMigratableInstances(WfInstanceMigrationQueryDTO queryDTO) {
        return R.success(migrationService.listMigratableInstances(queryDTO));
    }

    @Operation(summary = "兼容性检查")
    @GetMapping("/check-compatibility")
    public R<CompatibilityCheckVO> checkCompatibility(
            @RequestParam Long instanceId,
            @RequestParam Long targetVersionId) {
        return R.success(migrationService.checkCompatibility(instanceId, targetVersionId));
    }

    @Operation(summary = "批量迁移流程实例")
    @PostMapping("/batch-migrate")
    public R<WfProcessMigrationResultVO> batchMigrate(@Valid @RequestBody WfProcessMigrationDTO dto) {
        return R.success(migrationService.batchMigrate(dto));
    }

    @Operation(summary = "获取迁移结果详情")
    @GetMapping("/result/{recordId}")
    public R<WfProcessMigrationResultVO> getMigrationResult(@PathVariable Long recordId) {
        return R.success(migrationService.getMigrationResult(recordId));
    }

    @Operation(summary = "分页查询迁移记录")
    @GetMapping("/records")
    public R<IPage<WfProcessMigrationRecordVO>> pageMigrationRecords(
            @RequestParam(required = false) Long processDefinitionId,
            PageQuery query) {
        return R.success(migrationService.pageMigrationRecords(processDefinitionId, query));
    }

    @Operation(summary = "下载迁移备份数据")
    @GetMapping("/backup/download/{recordId}")
    public void downloadBackup(@PathVariable Long recordId, HttpServletResponse response) {
        byte[] data = migrationService.downloadBackup(recordId);
        String fileName = "migration_backup_" + recordId + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
        response.setContentLength(data.length);
        try {
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw new RuntimeException("下载失败", e);
        }
    }
}
