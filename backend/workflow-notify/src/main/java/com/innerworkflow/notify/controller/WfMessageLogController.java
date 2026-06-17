package com.innerworkflow.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.innerworkflow.common.result.R;
import com.innerworkflow.notify.dto.MessageLogQueryDTO;
import com.innerworkflow.notify.service.WfMessageLogService;
import com.innerworkflow.notify.vo.MessageLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "消息日志管理")
@RestController
@RequestMapping("/notify/log")
public class WfMessageLogController {

    @Autowired
    private WfMessageLogService messageLogService;

    @Operation(summary = "分页查询消息日志列表")
    @GetMapping("/page")
    public R<Page<MessageLogVO>> page(MessageLogQueryDTO queryDTO) {
        return R.success(messageLogService.pageList(queryDTO));
    }

    @Operation(summary = "获取消息日志详情")
    @GetMapping("/{id}")
    public R<MessageLogVO> getDetail(@PathVariable Long id) {
        return R.success(messageLogService.getDetail(id));
    }

    @Operation(summary = "重试发送消息")
    @PostMapping("/{id}/retry")
    public R<Boolean> retry(@PathVariable Long id) {
        return R.success(messageLogService.retrySend(id));
    }

    @Operation(summary = "标记消息为已读")
    @PutMapping("/{id}/read")
    public R<Boolean> markRead(@PathVariable Long id) {
        return R.success(messageLogService.markRead(id));
    }
}
