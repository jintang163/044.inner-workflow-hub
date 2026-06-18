package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfCcAddDTO;
import com.innerworkflow.approval.dto.WfCcRemindDTO;
import com.innerworkflow.approval.dto.WfCcTaskQueryDTO;
import com.innerworkflow.approval.service.WfCcTaskService;
import com.innerworkflow.approval.vo.WfCcTaskVO;
import com.innerworkflow.common.result.R;
import com.innerworkflow.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "抄送管理", description = "抄送相关操作接口")
@RestController
@RequestMapping("/approval/cc")
@RequiredArgsConstructor
public class WfCcController {

    private final WfCcTaskService ccTaskService;

    @Operation(summary = "添加抄送人（运行时）")
    @PostMapping("/add")
    public R<Void> addCc(@RequestBody WfCcAddDTO addDTO) {
        ccTaskService.addCc(addDTO);
        return R.success();
    }

    @Operation(summary = "催读（发起人催读未读抄送人）")
    @PostMapping("/remind")
    public R<Void> remind(@RequestBody WfCcRemindDTO remindDTO) {
        ccTaskService.remind(remindDTO);
        return R.success();
    }

    @Operation(summary = "查询流程实例的抄送列表（含已读/未读状态，仅发起人可见）")
    @GetMapping("/list/{instanceId}")
    public R<List<WfCcTaskVO>> listByInstanceId(@PathVariable Long instanceId) {
        return R.success(ccTaskService.listVOByInstanceId(instanceId));
    }

    @Operation(summary = "分页查询我的抄送列表")
    @GetMapping("/my-page")
    public R<IPage<WfCcTaskVO>> myCcPage(WfCcTaskQueryDTO queryDTO) {
        return R.success(ccTaskService.page(queryDTO));
    }

    @Operation(summary = "标记抄送为已读（单个）")
    @PostMapping("/mark-read/{id}")
    public R<Void> markRead(@PathVariable Long id) {
        ccTaskService.markRead(id);
        return R.success();
    }

    @Operation(summary = "批量标记抄送为已读")
    @PostMapping("/mark-read-batch")
    public R<Void> markReadBatch(@RequestBody List<Long> ids) {
        ccTaskService.markReadBatch(ids);
        return R.success();
    }

    @Operation(summary = "统计我的未读抄送数量")
    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        long count = ccTaskService.countUnreadByUserId(userId);
        return R.success(Map.of("count", count));
    }
}
