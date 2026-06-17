package com.innerworkflow.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.innerworkflow.common.result.R;
import com.innerworkflow.notify.dto.MessageTemplateQueryDTO;
import com.innerworkflow.notify.dto.MessageTemplateSaveDTO;
import com.innerworkflow.notify.service.WfMessageTemplateService;
import com.innerworkflow.notify.vo.MessageTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "消息模板管理")
@RestController
@RequestMapping("/notify/template")
public class WfMessageTemplateController {

    @Autowired
    private WfMessageTemplateService messageTemplateService;

    @Operation(summary = "分页查询消息模板列表")
    @GetMapping("/page")
    public R<Page<MessageTemplateVO>> page(MessageTemplateQueryDTO queryDTO) {
        return R.success(messageTemplateService.pageList(queryDTO));
    }

    @Operation(summary = "获取消息模板详情")
    @GetMapping("/{id}")
    public R<MessageTemplateVO> getDetail(@PathVariable Long id) {
        return R.success(messageTemplateService.getDetail(id));
    }

    @Operation(summary = "保存消息模板")
    @PostMapping
    public R<Boolean> save(@Valid @RequestBody MessageTemplateSaveDTO saveDTO) {
        return R.success(messageTemplateService.saveTemplate(saveDTO));
    }

    @Operation(summary = "更新模板状态")
    @PutMapping("/{id}/status")
    public R<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return R.success(messageTemplateService.updateStatus(id, status));
    }

    @Operation(summary = "删除消息模板")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.success(messageTemplateService.deleteTemplate(id));
    }
}
