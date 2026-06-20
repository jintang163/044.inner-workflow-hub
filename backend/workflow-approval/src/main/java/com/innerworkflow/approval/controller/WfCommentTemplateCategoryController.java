package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfCommentTemplateCategoryQueryDTO;
import com.innerworkflow.approval.dto.WfCommentTemplateCategorySaveDTO;
import com.innerworkflow.approval.service.WfCommentTemplateCategoryService;
import com.innerworkflow.approval.vo.WfCommentTemplateCategoryVO;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "意见模板分类管理", description = "审批意见模板分类相关接口")
@RestController
@RequestMapping("/approval/comment-template-category")
@RequiredArgsConstructor
public class WfCommentTemplateCategoryController {

    private final WfCommentTemplateCategoryService categoryService;

    @Operation(summary = "分页查询分类列表")
    @GetMapping("/page")
    public R<IPage<WfCommentTemplateCategoryVO>> page(WfCommentTemplateCategoryQueryDTO queryDTO) {
        return R.success(categoryService.page(queryDTO));
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    public R<WfCommentTemplateCategoryVO> getDetail(@PathVariable Long id) {
        return R.success(categoryService.getDetail(id));
    }

    @Operation(summary = "获取可用分类列表（当前用户可见的）")
    @GetMapping("/available")
    public R<List<WfCommentTemplateCategoryVO>> listAvailable() {
        return R.success(categoryService.listAvailable());
    }

    @Operation(summary = "按范围获取分类列表")
    @GetMapping("/scope/{scopeType}")
    public R<List<WfCommentTemplateCategoryVO>> listByScope(@PathVariable Integer scopeType) {
        return R.success(categoryService.listByScope(scopeType));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public R<Void> save(@RequestBody @Valid WfCommentTemplateCategorySaveDTO saveDTO) {
        categoryService.save(saveDTO);
        return R.success();
    }

    @Operation(summary = "更新分类")
    @PutMapping
    public R<Void> update(@RequestBody @Valid WfCommentTemplateCategorySaveDTO saveDTO) {
        categoryService.update(saveDTO);
        return R.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.success();
    }

    @Operation(summary = "更新分类状态")
    @PutMapping("/status/{id}")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        categoryService.updateStatus(id, status);
        return R.success();
    }
}
