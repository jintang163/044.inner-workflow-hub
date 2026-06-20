package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.service.WfAttachmentService;
import com.innerworkflow.approval.service.WfSealConfigService;
import com.innerworkflow.approval.vo.WfAttachmentVO;
import com.innerworkflow.common.result.R;
import com.innerworkflow.form.dto.WfSealConfigSaveDTO;
import com.innerworkflow.form.vo.WfSealConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "电子印章配置", description = "电子印章和国密数字签名配置")
@RestController
@RequestMapping("/approval/seal")
@RequiredArgsConstructor
public class WfSealController {

    private final WfSealConfigService sealConfigService;
    private final WfAttachmentService attachmentService;

    @Operation(summary = "上传印章图片")
    @PostMapping("/image/upload")
    public R<WfAttachmentVO> uploadSealImage(@RequestParam("file") MultipartFile file) {
        return R.success(attachmentService.upload(file, "seal_image", null, null));
    }

    @Operation(summary = "上传国密数字证书(p12)")
    @PostMapping("/cert/upload")
    public R<WfAttachmentVO> uploadCert(@RequestParam("file") MultipartFile file) {
        return R.success(attachmentService.upload(file, "seal_cert", null, null));
    }

    @Operation(summary = "印章列表")
    @GetMapping("/list")
    public R<List<WfSealConfigVO>> list(@RequestParam(required = false) Integer sealType) {
        return R.success(sealConfigService.list(sealType));
    }

    @Operation(summary = "印章分页")
    @GetMapping("/page")
    public R<IPage<WfSealConfigVO>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer sealType) {
        return R.success(sealConfigService.page(current, size, keyword, sealType));
    }

    @Operation(summary = "印章详情")
    @GetMapping("/{id}")
    public R<WfSealConfigVO> getDetail(@PathVariable Long id) {
        return R.success(sealConfigService.getDetail(id));
    }

    @Operation(summary = "保存印章")
    @PostMapping
    public R<WfSealConfigVO> save(@Valid @RequestBody WfSealConfigSaveDTO dto) {
        return R.success(sealConfigService.save(dto));
    }

    @Operation(summary = "删除印章")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sealConfigService.remove(id);
        return R.success();
    }
}
