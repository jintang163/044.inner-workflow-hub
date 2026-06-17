package com.innerworkflow.approval.controller;

import com.innerworkflow.approval.entity.WfAttachment;
import com.innerworkflow.approval.service.WfAttachmentService;
import com.innerworkflow.approval.vo.WfAttachmentVO;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "附件管理", description = "附件上传下载相关接口")
@RestController
@RequestMapping("/approval/attachment")
@RequiredArgsConstructor
public class WfAttachmentController {

    private final WfAttachmentService attachmentService;

    @Operation(summary = "上传附件")
    @PostMapping("/upload")
    public R<WfAttachmentVO> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(required = false) String bizType,
                                    @RequestParam(required = false) String bizId) {
        return R.success(attachmentService.upload(file, bizType, bizId));
    }

    @Operation(summary = "获取附件详情")
    @GetMapping("/{id}")
    public R<WfAttachmentVO> getById(@PathVariable Long id) {
        WfAttachment attachment = attachmentService.getById(id);
        WfAttachmentVO vo = new WfAttachmentVO();
        org.springframework.beans.BeanUtils.copyProperties(attachment, vo);
        return R.success(vo);
    }

    @Operation(summary = "根据业务ID获取附件列表")
    @GetMapping("/list")
    public R<List<WfAttachmentVO>> listByBiz(@RequestParam String bizType, @RequestParam String bizId) {
        return R.success(attachmentService.listByBiz(bizType, bizId));
    }

    @Operation(summary = "根据ID列表获取附件")
    @GetMapping("/listByIds")
    public R<List<WfAttachmentVO>> listByIds(@RequestParam List<Long> ids) {
        return R.success(attachmentService.listByIds(ids));
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        attachmentService.removeById(id);
        return R.success();
    }
}
