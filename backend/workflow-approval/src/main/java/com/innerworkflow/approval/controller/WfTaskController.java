package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfCcTaskQueryDTO;
import com.innerworkflow.approval.dto.WfDoneTaskQueryDTO;
import com.innerworkflow.approval.dto.WfTodoTaskQueryDTO;
import com.innerworkflow.approval.service.WfApprovalTaskService;
import com.innerworkflow.approval.service.WfCcTaskService;
import com.innerworkflow.approval.vo.WfApprovalTaskVO;
import com.innerworkflow.approval.vo.WfCcTaskVO;
import com.innerworkflow.common.result.R;
import com.innerworkflow.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "任务查询", description = "任务查询相关接口")
@RestController
@RequestMapping("/approval/task")
@RequiredArgsConstructor
public class WfTaskController {

    private final WfApprovalTaskService approvalTaskService;
    private final WfCcTaskService ccTaskService;

    @Operation(summary = "待办任务列表")
    @GetMapping("/todo")
    public R<IPage<WfApprovalTaskVO>> todoList(WfTodoTaskQueryDTO queryDTO) {
        return R.success(approvalTaskService.pageTodo(queryDTO));
    }

    @Operation(summary = "已办任务列表")
    @GetMapping("/done")
    public R<IPage<WfApprovalTaskVO>> doneList(WfDoneTaskQueryDTO queryDTO) {
        return R.success(approvalTaskService.pageDone(queryDTO));
    }

    @Operation(summary = "抄送任务列表")
    @GetMapping("/cc")
    public R<IPage<WfCcTaskVO>> ccList(WfCcTaskQueryDTO queryDTO) {
        return R.success(ccTaskService.page(queryDTO));
    }

    @Operation(summary = "标记抄送已读")
    @PutMapping("/cc/{id}/read")
    public R<Void> markCcRead(@PathVariable Long id) {
        ccTaskService.markRead(id);
        return R.success();
    }

    @Operation(summary = "获取任务数量统计")
    @GetMapping("/count")
    public R<Map<String, Long>> getTaskCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Long> countMap = new HashMap<>();
        countMap.put("todoCount", approvalTaskService.countTodoByUserId(userId));
        countMap.put("ccUnreadCount", ccTaskService.countUnreadByUserId(userId));
        return R.success(countMap);
    }
}
