package com.innerworkflow.bpmn.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.bpmn.dto.WfCategoryQueryDTO;
import com.innerworkflow.bpmn.entity.WfCategory;
import com.innerworkflow.bpmn.service.WfCategoryService;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "流程分类管理", description = "流程分类相关接口")
@RestController
@RequestMapping("/bpmn/category")
@RequiredArgsConstructor
public class WfCategoryController {

    private final WfCategoryService categoryService;

    @Operation(summary = "分页查询流程分类")
    @GetMapping("/page")
    public R<IPage<WfCategory>> page(WfCategoryQueryDTO queryDTO) {
        return R.success(categoryService.page(queryDTO));
    }

    @Operation(summary = "根据业务线ID获取分类列表")
    @GetMapping("/list/{businessLineId}")
    public R<List<WfCategory>> listByBusinessLineId(@PathVariable Long businessLineId) {
        return R.success(categoryService.listByBusinessLineId(businessLineId));
    }

    @Operation(summary = "根据ID获取分类详情")
    @GetMapping("/{id}")
    public R<WfCategory> getById(@PathVariable Long id) {
        return R.success(categoryService.getById(id));
    }

    @Operation(summary = "新增流程分类")
    @PostMapping
    public R<Void> save(@RequestBody WfCategory category) {
        categoryService.save(category);
        return R.success();
    }

    @Operation(summary = "修改流程分类")
    @PutMapping
    public R<Void> update(@RequestBody WfCategory category) {
        categoryService.updateById(category);
        return R.success();
    }

    @Operation(summary = "删除流程分类")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        categoryService.removeById(id);
        return R.success();
    }
}
