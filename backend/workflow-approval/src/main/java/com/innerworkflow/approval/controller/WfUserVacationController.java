package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfUserVacationQueryDTO;
import com.innerworkflow.approval.dto.WfUserVacationSaveDTO;
import com.innerworkflow.approval.entity.WfUserVacation;
import com.innerworkflow.approval.service.WfUserVacationService;
import com.innerworkflow.common.result.R;
import com.innerworkflow.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "休假管理", description = "用户休假管理相关接口")
@RestController
@RequestMapping("/approval/vacation")
@RequiredArgsConstructor
public class WfUserVacationController {

    private final WfUserVacationService userVacationService;
    private final WfApprovalService approvalService;

    @Operation(summary = "分页查询休假记录")
    @GetMapping("/page")
    public R<IPage<WfUserVacation>> page(WfUserVacationQueryDTO queryDTO) {
        return R.success(userVacationService.page(queryDTO));
    }

    @Operation(summary = "根据ID查询休假记录")
    @GetMapping("/{id}")
    public R<WfUserVacation> getById(@PathVariable Long id) {
        return R.success(userVacationService.getById(id));
    }

    @Operation(summary = "查询当前用户休假记录")
    @GetMapping("/my/list")
    public R<List<WfUserVacation>> getMyVacations(
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.success(userVacationService.listByUserAndTimeRange(userId, startTime, endTime));
    }

    @Operation(summary = "查询用户当前休假状态")
    @GetMapping("/current/{userId}")
    public R<WfUserVacation> getCurrentVacation(@PathVariable Long userId) {
        return R.success(userVacationService.getCurrentVacation(userId));
    }

    @Operation(summary = "查询我的当前休假状态")
    @GetMapping("/my/current")
    public R<WfUserVacation> getMyCurrentVacation() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.success(userVacationService.getCurrentVacation(userId));
    }

    @Operation(summary = "检查用户是否在休假")
    @GetMapping("/check/{userId}")
    public R<Boolean> checkUserOnVacation(
            @PathVariable Long userId,
            @RequestParam(required = false) LocalDateTime time) {
        if (time == null) {
            time = LocalDateTime.now();
        }
        return R.success(userVacationService.isUserOnVacation(userId, time));
    }

    @Operation(summary = "新增休假记录")
    @PostMapping
    public R<Void> save(@Valid @RequestBody WfUserVacationSaveDTO dto) {
        userVacationService.save(dto);
        return R.success();
    }

    @Operation(summary = "修改休假记录")
    @PutMapping
    public R<Void> update(@Valid @RequestBody WfUserVacationSaveDTO dto) {
        userVacationService.update(dto);
        return R.success();
    }

    @Operation(summary = "删除休假记录")
    @DeleteMapping("/{id}")
    public R<Void> deleteById(@PathVariable Long id) {
        userVacationService.deleteById(id);
        return R.success();
    }

    @Operation(summary = "取消休假")
    @PostMapping("/cancel/{id}")
    public R<Void> cancelVacation(@PathVariable Long id) {
        userVacationService.cancelVacation(id);
        return R.success();
    }

    @Operation(summary = "从数据源同步休假")
    @PostMapping("/sync/{sourceType}")
    public R<Void> syncVacation(@PathVariable String sourceType) {
        Long userId = SecurityUtils.getCurrentUserId();
        userVacationService.syncVacationFromSource(userId, sourceType);
        return R.success();
    }

    @Operation(summary = "为指定用户同步休假")
    @PostMapping("/sync/user/{userId}/{sourceType}")
    public R<Void> syncUserVacation(
            @PathVariable Long userId,
            @PathVariable String sourceType) {
        userVacationService.syncVacationFromSource(userId, sourceType);
        return R.success();
    }

    @Operation(summary = "手动触发休假待办批量转派")
    @PostMapping("/batch-transfer")
    public R<Integer> batchTransferVacationTasks() {
        return R.success(approvalService.batchTransferVacationUsers());
    }

    @Operation(summary = "手动触发指定休假记录的待办转派")
    @PostMapping("/transfer/{vacationId}")
    public R<Void> transferVacationTasks(@PathVariable Long vacationId) {
        approvalService.transferExistingTasksForVacation(vacationId);
        return R.success();
    }
}
