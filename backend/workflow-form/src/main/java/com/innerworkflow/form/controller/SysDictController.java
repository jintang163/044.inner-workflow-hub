package com.innerworkflow.form.controller;

import com.innerworkflow.common.result.R;
import com.innerworkflow.form.dto.SysDictDataSaveDTO;
import com.innerworkflow.form.dto.SysDictTypeSaveDTO;
import com.innerworkflow.form.service.SysDictDataService;
import com.innerworkflow.form.service.SysDictTypeService;
import com.innerworkflow.form.vo.SysDictDataVO;
import com.innerworkflow.form.vo.SysDictTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "数据字典管理")
@RestController
@RequestMapping("/system/dict")
public class SysDictController {

    @Autowired
    private SysDictTypeService dictTypeService;

    @Autowired
    private SysDictDataService dictDataService;

    @Operation(summary = "查询所有字典类型")
    @GetMapping("/type/list")
    public R<List<SysDictTypeVO>> listDictTypes() {
        return R.success(dictTypeService.listAll());
    }

    @Operation(summary = "获取字典类型详情")
    @GetMapping("/type/{id}")
    public R<SysDictTypeVO> getDictTypeDetail(@PathVariable Long id) {
        return R.success(dictTypeService.getDetail(id));
    }

    @Operation(summary = "保存字典类型")
    @PostMapping("/type")
    public R<Boolean> saveDictType(@Valid @RequestBody SysDictTypeSaveDTO saveDTO) {
        return R.success(dictTypeService.saveDictType(saveDTO));
    }

    @Operation(summary = "更新字典类型")
    @PutMapping("/type")
    public R<Boolean> updateDictType(@Valid @RequestBody SysDictTypeSaveDTO saveDTO) {
        return R.success(dictTypeService.updateDictType(saveDTO));
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/type/{id}")
    public R<Boolean> deleteDictType(@PathVariable Long id) {
        return R.success(dictTypeService.deleteDictType(id));
    }

    @Operation(summary = "根据字典编码获取字典数据")
    @GetMapping("/data/{dictCode}")
    public R<List<SysDictDataVO>> getDictData(@PathVariable String dictCode) {
        return R.success(dictTypeService.getDictDataByCode(dictCode));
    }

    @Operation(summary = "获取级联字典数据(根据父级值)")
    @GetMapping("/data/{dictCode}/cascade")
    public R<List<SysDictDataVO>> getCascadeDictData(
            @PathVariable String dictCode,
            @RequestParam(required = false) String parentValue) {
        return R.success(dictTypeService.getCascadeDictData(dictCode, parentValue));
    }

    @Operation(summary = "保存字典数据")
    @PostMapping("/data")
    public R<Boolean> saveDictData(@Valid @RequestBody SysDictDataSaveDTO saveDTO) {
        return R.success(dictDataService.saveDictData(saveDTO));
    }

    @Operation(summary = "更新字典数据")
    @PutMapping("/data")
    public R<Boolean> updateDictData(@Valid @RequestBody SysDictDataSaveDTO saveDTO) {
        return R.success(dictDataService.updateDictData(saveDTO));
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping("/data/{id}")
    public R<Boolean> deleteDictData(@PathVariable Long id) {
        return R.success(dictDataService.deleteDictData(id));
    }

    @Operation(summary = "刷新字典缓存")
    @PostMapping("/cache/refresh/{dictCode}")
    public R<Void> refreshCache(@PathVariable String dictCode) {
        dictTypeService.refreshCache(dictCode);
        return R.success();
    }

    @Operation(summary = "清除所有字典缓存")
    @PostMapping("/cache/clear")
    public R<Void> clearAllCache() {
        dictTypeService.clearAllDictCache();
        return R.success();
    }
}
