package com.innerworkflow.form.controller;

import com.innerworkflow.common.result.R;
import com.innerworkflow.form.dto.WfDataSourceConfigSaveDTO;
import com.innerworkflow.form.service.WfDataSourceConfigService;
import com.innerworkflow.form.vo.WfDataSourceConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "API数据源配置管理")
@RestController
@RequestMapping("/system/datasource")
public class WfDataSourceConfigController {

    @Autowired
    private WfDataSourceConfigService dataSourceConfigService;

    @Operation(summary = "查询所有数据源配置")
    @GetMapping("/list")
    public R<List<WfDataSourceConfigVO>> listAll() {
        return R.success(dataSourceConfigService.listAll());
    }

    @Operation(summary = "获取数据源配置详情")
    @GetMapping("/{id}")
    public R<WfDataSourceConfigVO> getDetail(@PathVariable Long id) {
        return R.success(dataSourceConfigService.getDetail(id));
    }

    @Operation(summary = "保存数据源配置")
    @PostMapping
    public R<Boolean> save(@Valid @RequestBody WfDataSourceConfigSaveDTO saveDTO) {
        return R.success(dataSourceConfigService.saveDataSourceConfig(saveDTO));
    }

    @Operation(summary = "更新数据源配置")
    @PutMapping
    public R<Boolean> update(@Valid @RequestBody WfDataSourceConfigSaveDTO saveDTO) {
        return R.success(dataSourceConfigService.updateDataSourceConfig(saveDTO));
    }

    @Operation(summary = "删除数据源配置")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.success(dataSourceConfigService.deleteDataSourceConfig(id));
    }

    @Operation(summary = "调用API获取动态数据")
    @PostMapping("/fetch/{sourceCode}")
    public R<List<Map<String, Object>>> fetchApiData(
            @PathVariable String sourceCode,
            @RequestBody(required = false) Map<String, Object> params) {
        return R.success(dataSourceConfigService.fetchApiData(sourceCode, params));
    }

    @Operation(summary = "刷新数据源缓存")
    @PostMapping("/cache/refresh/{sourceCode}")
    public R<Void> refreshCache(@PathVariable String sourceCode) {
        dataSourceConfigService.refreshCache(sourceCode);
        return R.success();
    }
}
