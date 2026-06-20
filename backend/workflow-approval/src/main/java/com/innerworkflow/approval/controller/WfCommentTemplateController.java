package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfCommentTemplateQueryDTO;
import com.innerworkflow.approval.dto.WfCommentTemplateSaveDTO;
import com.innerworkflow.approval.service.WfCommentTemplateService;
import com.innerworkflow.approval.vo.WfCommentTemplateVO;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "意见模板管理", description = "审批意见模板相关接口")
@RestController
@RequestMapping("/approval/comment-template")
@RequiredArgsConstructor
public class WfCommentTemplateController {

    private final WfCommentTemplateService templateService;

    @Operation(summary = "分页查询模板列表")
    @GetMapping("/page")
    public R<IPage<WfCommentTemplateVO>> page(WfCommentTemplateQueryDTO queryDTO) {
        return R.success(templateService.page(queryDTO));
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    public R<WfCommentTemplateVO> getDetail(@PathVariable Long id) {
        return R.success(templateService.getDetail(id));
    }

    @Operation(summary = "获取当前用户可用的所有模板（个人+部门+全局）")
    @GetMapping("/my-available")
    public R<List<WfCommentTemplateVO>> listMyAvailable() {
        return R.success(templateService.listMyAvailable());
    }

    @Operation(summary = "按分类ID获取模板列表")
    @GetMapping("/category/{categoryId}")
    public R<List<WfCommentTemplateVO>> listByCategoryId(@PathVariable Long categoryId) {
        return R.success(templateService.listByCategoryId(categoryId));
    }

    @Operation(summary = "按范围获取模板列表")
    @GetMapping("/scope/{scopeType}")
    public R<List<WfCommentTemplateVO>> listByScopeType(@PathVariable Integer scopeType) {
        return R.success(templateService.listByScopeType(scopeType));
    }

    @Operation(summary = "新增模板")
    @PostMapping
    public R<Void> save(@RequestBody @Valid WfCommentTemplateSaveDTO saveDTO) {
        templateService.save(saveDTO);
        return R.success();
    }

    @Operation(summary = "更新模板")
    @PutMapping
    public R<Void> update(@RequestBody @Valid WfCommentTemplateSaveDTO saveDTO) {
        templateService.update(saveDTO);
        return R.success();
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return R.success();
    }

    @Operation(summary = "更新模板状态")
    @PutMapping("/status/{id}")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        templateService.updateStatus(id, status);
        return R.success();
    }

    @Operation(summary = "记录模板使用次数（使用模板时调用）")
    @PostMapping("/use/{id}")
    public R<Void> useTemplate(@PathVariable Long id) {
        templateService.incrementUseCount(id);
        return R.success();
    }
}
