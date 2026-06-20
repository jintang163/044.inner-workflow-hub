package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfBatchRemindDTO;
import com.innerworkflow.approval.dto.WfTaskRemindDTO;
import com.innerworkflow.approval.entity.WfEscalationHistory;
import com.innerworkflow.approval.entity.WfTimeoutRemind;
import com.innerworkflow.approval.service.WfEscalationHistoryService;
import com.innerworkflow.approval.service.WfTimeoutRemindService;
import com.innerworkflow.approval.vo.WfBatchRemindResultVO;
import com.innerworkflow.common.dto.PageQuery;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "催办与升级", description = "催办与升级相关接口")
@RestController
@RequestMapping("/approval/remind")
@RequiredArgsConstructor
public class WfRemindController {

    private final WfTimeoutRemindService timeoutRemindService;
    private final WfEscalationHistoryService escalationHistoryService;

    @Operation(summary = "手动催办任务")
    @PostMapping("/manual")
    public R<Void> manualRemind(@Valid @RequestBody WfTaskRemindDTO dto) {
        timeoutRemindService.manualRemind(dto.getTaskId(), dto.getRemark());
        return R.success();
    }

    @Operation(summary = "批量催办任务")
    @PostMapping("/batch")
    public R<WfBatchRemindResultVO> batchRemind(@Valid @RequestBody WfBatchRemindDTO dto) {
        return R.success(timeoutRemindService.batchRemind(dto));
    }

    @Operation(summary = "获取任务上次催办时间")
    @GetMapping("/{taskId}/last-time")
    public R<LocalDateTime> getLastRemindTime(@PathVariable Long taskId) {
        return R.success(timeoutRemindService.getLastRemindTime(taskId));
    }

    @Operation(summary = "检查是否可以催办（间隔校验）")
    @GetMapping("/{taskId}/can-remind")
    public R<Boolean> canRemind(@PathVariable Long taskId,
                                @RequestParam(defaultValue = "5") Integer intervalMinutes) {
        return R.success(timeoutRemindService.checkRemindInterval(taskId, intervalMinutes));
    }

    @Operation(summary = "查询任务催办记录(分页)")
    @GetMapping("/task/{taskId}/page")
    public R<IPage<WfTimeoutRemind>> getRemindPageByTaskId(@PathVariable Long taskId, PageQuery query) {
        return R.success(timeoutRemindService.pageByTaskId(taskId, query));
    }

    @Operation(summary = "查询任务催办记录(列表)")
    @GetMapping("/task/{taskId}/list")
    public R<List<WfTimeoutRemind>> getRemindListByTaskId(@PathVariable Long taskId) {
        return R.success(timeoutRemindService.listByTaskId(taskId));
    }

    @Operation(summary = "查询实例催办记录(分页)")
    @GetMapping("/instance/{instanceId}/page")
    public R<IPage<WfTimeoutRemind>> getRemindPageByInstanceId(@PathVariable Long instanceId, PageQuery query) {
        return R.success(timeoutRemindService.pageByInstanceId(instanceId, query));
    }

    @Operation(summary = "查询实例催办记录(列表)")
    @GetMapping("/instance/{instanceId}/list")
    public R<List<WfTimeoutRemind>> getRemindListByInstanceId(@PathVariable Long instanceId) {
        return R.success(timeoutRemindService.listByInstanceId(instanceId));
    }

    @Operation(summary = "查询任务升级历史(列表)")
    @GetMapping("/escalation/task/{taskId}")
    public R<List<WfEscalationHistory>> getEscalationHistoryByTaskId(@PathVariable Long taskId) {
        return R.success(escalationHistoryService.listByTaskId(taskId));
    }

    @Operation(summary = "查询任务升级历史(分页)")
    @GetMapping("/escalation/task/{taskId}/page")
    public R<IPage<WfEscalationHistory>> getEscalationHistoryPageByTaskId(
            @PathVariable Long taskId, PageQuery query) {
        return R.success(escalationHistoryService.pageByTaskId(taskId, query));
    }

    @Operation(summary = "查询实例升级历史(列表)")
    @GetMapping("/escalation/instance/{instanceId}")
    public R<List<WfEscalationHistory>> getEscalationHistoryByInstanceId(@PathVariable Long instanceId) {
        return R.success(escalationHistoryService.listByInstanceId(instanceId));
    }

    @Operation(summary = "查询实例升级历史(分页)")
    @GetMapping("/escalation/instance/{instanceId}/page")
    public R<IPage<WfEscalationHistory>> getEscalationHistoryPageByInstanceId(
            @PathVariable Long instanceId, PageQuery query) {
        return R.success(escalationHistoryService.pageByInstanceId(instanceId, query));
    }
}
