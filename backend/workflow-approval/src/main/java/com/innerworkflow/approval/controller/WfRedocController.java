package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.service.WfRedocTemplateService;
import com.innerworkflow.common.result.R;
import com.innerworkflow.form.dto.WfRedocBatchDTO;
import com.innerworkflow.form.dto.WfRedocGenerateDTO;
import com.innerworkflow.form.dto.WfRedocTemplateSaveDTO;
import com.innerworkflow.form.vo.WfRedocGeneratedVO;
import com.innerworkflow.form.vo.WfRedocTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Tag(name = "红头文件模板与生成", description = "红头文件模板管理、生成、批量打印下载")
@RestController
@RequestMapping("/approval/redoc")
@RequiredArgsConstructor
public class WfRedocController {

    private final WfRedocTemplateService redocTemplateService;

    @Operation(summary = "模板列表")
    @GetMapping("/template/list")
    public R<List<WfRedocTemplateVO>> listTemplate(@RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String processKey) {
        return R.success(redocTemplateService.list(category, processKey));
    }

    @Operation(summary = "模板分页")
    @GetMapping("/template/page")
    public R<IPage<WfRedocTemplateVO>> pageTemplate(@RequestParam(defaultValue = "1") long current,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String category) {
        return R.success(redocTemplateService.page(current, size, keyword, category));
    }

    @Operation(summary = "模板详情")
    @GetMapping("/template/{id}")
    public R<WfRedocTemplateVO> getTemplateDetail(@PathVariable Long id) {
        return R.success(redocTemplateService.getDetail(id));
    }

    @Operation(summary = "保存模板")
    @PostMapping("/template")
    public R<WfRedocTemplateVO> saveTemplate(@Valid @RequestBody WfRedocTemplateSaveDTO dto) {
        return R.success(redocTemplateService.save(dto));
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/template/{id}")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        redocTemplateService.remove(id);
        return R.success();
    }

    @Operation(summary = "提取模板占位符")
    @GetMapping("/template/{id}/placeholders")
    public R<Set<String>> extractPlaceholders(@PathVariable Long id) {
        return R.success(redocTemplateService.extractPlaceholders(id));
    }

    @Operation(summary = "生成红头文件")
    @PostMapping("/generate")
    public R<WfRedocGeneratedVO> generate(@Valid @RequestBody WfRedocGenerateDTO dto) {
        return R.success(redocTemplateService.generate(dto));
    }

    @Operation(summary = "根据审批单号查询生成记录")
    @GetMapping("/generated/list")
    public R<List<WfRedocGeneratedVO>> listByInstance(@RequestParam String instanceNo) {
        return R.success(redocTemplateService.listByInstance(instanceNo));
    }

    @Operation(summary = "生成记录分页")
    @GetMapping("/generated/page")
    public R<IPage<WfRedocGeneratedVO>> pageGenerated(@RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) String instanceNo,
                                                       @RequestParam(required = false) String templateId) {
        return R.success(redocTemplateService.pageGenerated(current, size, instanceNo, templateId));
    }

    @Operation(summary = "生成记录详情")
    @GetMapping("/generated/{id}")
    public R<WfRedocGeneratedVO> getGeneratedDetail(@PathVariable Long id) {
        return R.success(redocTemplateService.getGeneratedDetail(id));
    }

    @Operation(summary = "标记已打印")
    @PostMapping("/generated/{id}/printed")
    public R<Boolean> markPrinted(@PathVariable Long id) {
        return R.success(redocTemplateService.markPrinted(id));
    }

    @Operation(summary = "标记已下载")
    @PostMapping("/generated/{id}/downloaded")
    public R<Boolean> markDownloaded(@PathVariable Long id) {
        return R.success(redocTemplateService.markDownloaded(id));
    }

    @Operation(summary = "批量打印(合并为单个PDF下载)")
    @PostMapping("/batch/print")
    public ResponseEntity<byte[]> batchPrint(@RequestBody WfRedocBatchDTO dto) {
        byte[] pdf = redocTemplateService.batchPrintPdf(dto);
        String filename = URLEncoder.encode("红头文件_批量打印.pdf", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @Operation(summary = "批量下载")
    @PostMapping("/batch/download")
    public ResponseEntity<byte[]> batchDownload(@RequestBody WfRedocBatchDTO dto) {
        byte[] data = redocTemplateService.batchDownload(dto);
        String filename = URLEncoder.encode("红头文件_批量下载.zip", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(data.length)
                .body(data);
    }
}
