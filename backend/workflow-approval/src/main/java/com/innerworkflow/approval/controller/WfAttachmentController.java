package com.innerworkflow.approval.controller;

import com.innerworkflow.approval.entity.WfAttachment;
import com.innerworkflow.approval.entity.WfAttachmentPermission;
import com.innerworkflow.approval.service.WfAttachmentService;
import com.innerworkflow.approval.vo.WfAttachmentVO;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Tag(name = "附件管理", description = "附件上传下载预览相关接口")
@RestController
@RequestMapping("/approval/attachment")
@RequiredArgsConstructor
public class WfAttachmentController {

    private final WfAttachmentService attachmentService;

    @Operation(summary = "上传附件")
    @PostMapping("/upload")
    public R<WfAttachmentVO> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(required = false) String bizType,
                                    @RequestParam(required = false) String bizId,
                                    @RequestParam(required = false) String nodeId) {
        return R.success(attachmentService.upload(file, bizType, bizId, nodeId));
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
    public R<List<WfAttachmentVO>> listByBiz(@RequestParam String bizType,
                                              @RequestParam String bizId,
                                              @RequestParam(required = false) String nodeId,
                                              @RequestParam(required = false) Long processVersionId) {
        if (processVersionId != null || nodeId != null) {
            return R.success(attachmentService.listByBizWithPermission(bizType, bizId, processVersionId, nodeId));
        }
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

    @Operation(summary = "获取附件预览URL(图片/PDF)")
    @GetMapping("/{id}/preview")
    public R<String> getPreviewUrl(@PathVariable Long id) {
        return R.success(attachmentService.getPreviewUrl(id));
    }

    @Operation(summary = "获取附件下载URL")
    @GetMapping("/{id}/download-url")
    public R<String> getDownloadUrl(@PathVariable Long id) {
        return R.success(attachmentService.getDownloadUrl(id));
    }

    @Operation(summary = "多文件批量下载(ZIP)")
    @PostMapping("/batch-download")
    public ResponseEntity<byte[]> batchDownload(@RequestBody List<Long> ids) {
        byte[] zipBytes = attachmentService.batchDownload(ids);
        String fileName = URLEncoder.encode("attachments.zip", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }

    @Operation(summary = "获取节点附件权限配置")
    @GetMapping("/permission")
    public R<WfAttachmentPermission> getPermission(@RequestParam Long processVersionId,
                                                    @RequestParam String nodeId) {
        return R.success(attachmentService.getPermission(processVersionId, nodeId));
    }

    @Operation(summary = "保存节点附件权限配置")
    @PostMapping("/permission")
    public R<WfAttachmentPermission> savePermission(@RequestBody WfAttachmentPermission permission) {
        return R.success(attachmentService.savePermission(permission));
    }

    @Operation(summary = "从节点表单权限同步附件权限")
    @PostMapping("/permission/sync")
    public R<Void> syncPermission(@RequestParam Long processVersionId,
                                   @RequestParam String nodeId,
                                   @RequestBody Map<String, String> formPermission) {
        attachmentService.syncPermissionFromNodeConfig(processVersionId, nodeId, formPermission);
        return R.success();
    }
}
