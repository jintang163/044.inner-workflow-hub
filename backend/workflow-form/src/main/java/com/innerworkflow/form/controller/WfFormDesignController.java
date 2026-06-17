package com.innerworkflow.form.controller;

import com.innerworkflow.common.result.R;
import com.innerworkflow.form.dto.FieldPermissionCalcDTO;
import com.innerworkflow.form.dto.FormPublishDTO;
import com.innerworkflow.form.service.WfFormFieldService;
import com.innerworkflow.form.service.WfFormVersionService;
import com.innerworkflow.form.vo.FieldPermissionVO;
import com.innerworkflow.form.vo.FormVersionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "表单设计管理")
@RestController
@RequestMapping("/form/design")
public class WfFormDesignController {

    @Autowired
    private WfFormVersionService formVersionService;

    @Autowired
    private WfFormFieldService formFieldService;

    @Operation(summary = "获取表单版本列表")
    @GetMapping("/versions/{formDefinitionId}")
    public R<List<FormVersionVO>> listVersions(@PathVariable Long formDefinitionId) {
        return R.success(formVersionService.listByFormDefinitionId(formDefinitionId));
    }

    @Operation(summary = "获取当前版本")
    @GetMapping("/versions/{formDefinitionId}/current")
    public R<FormVersionVO> getCurrentVersion(@PathVariable Long formDefinitionId) {
        return R.success(formVersionService.getCurrentVersion(formDefinitionId));
    }

    @Operation(summary = "获取指定版本详情")
    @GetMapping("/versions/{formDefinitionId}/{version}")
    public R<FormVersionVO> getVersion(@PathVariable Long formDefinitionId, @PathVariable Integer version) {
        return R.success(formVersionService.getByVersion(formDefinitionId, version));
    }

    @Operation(summary = "发布表单新版本")
    @PostMapping("/publish")
    public R<FormVersionVO> publish(@Valid @RequestBody FormPublishDTO publishDTO) {
        return R.success(formVersionService.publish(publishDTO));
    }

    @Operation(summary = "设置当前版本")
    @PutMapping("/versions/{formDefinitionId}/current/{versionId}")
    public R<Boolean> setCurrentVersion(@PathVariable Long formDefinitionId, @PathVariable Long versionId) {
        return R.success(formVersionService.setCurrentVersion(formDefinitionId, versionId));
    }

    @Operation(summary = "计算节点字段权限")
    @PostMapping("/field-permission/calc")
    public R<FieldPermissionVO> calcFieldPermissions(@Valid @RequestBody FieldPermissionCalcDTO calcDTO) {
        return R.success(formFieldService.calcFieldPermissions(calcDTO));
    }
}
