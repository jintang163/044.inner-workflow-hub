package com.innerworkflow.form.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.innerworkflow.common.result.R;
import com.innerworkflow.form.dto.FormDefinitionQueryDTO;
import com.innerworkflow.form.dto.FormDefinitionSaveDTO;
import com.innerworkflow.form.service.WfFormDefinitionService;
import com.innerworkflow.form.vo.FormDefinitionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "表单定义管理")
@RestController
@RequestMapping("/form/definition")
public class WfFormDefinitionController {

    @Autowired
    private WfFormDefinitionService formDefinitionService;

    @Operation(summary = "分页查询表单定义列表")
    @GetMapping("/page")
    public R<Page<FormDefinitionVO>> page(FormDefinitionQueryDTO queryDTO) {
        return R.success(formDefinitionService.pageList(queryDTO));
    }

    @Operation(summary = "获取表单定义详情")
    @GetMapping("/{id}")
    public R<FormDefinitionVO> getDetail(@PathVariable Long id) {
        return R.success(formDefinitionService.getDetail(id));
    }

    @Operation(summary = "保存表单定义")
    @PostMapping
    public R<Boolean> save(@Valid @RequestBody FormDefinitionSaveDTO saveDTO) {
        return R.success(formDefinitionService.saveForm(saveDTO));
    }

    @Operation(summary = "更新表单状态")
    @PutMapping("/{id}/status")
    public R<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return R.success(formDefinitionService.updateStatus(id, status));
    }

    @Operation(summary = "删除表单定义")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.success(formDefinitionService.deleteForm(id));
    }
}
