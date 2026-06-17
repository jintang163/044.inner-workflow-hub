package com.innerworkflow.auth.controller;

import com.innerworkflow.auth.dto.DeptSaveDTO;
import com.innerworkflow.auth.service.SysDeptService;
import com.innerworkflow.auth.vo.DeptVO;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService sysDeptService;

    @Operation(summary = "获取部门树")
    @GetMapping("/tree")
    public R<List<DeptVO>> getDeptTree() {
        List<DeptVO> tree = sysDeptService.getDeptTree();
        return R.success(tree);
    }

    @Operation(summary = "获取部门列表")
    @GetMapping("/list")
    public R<List<DeptVO>> getDeptList(
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) Integer status) {
        List<DeptVO> list = sysDeptService.getDeptList(deptName, status);
        return R.success(list);
    }

    @Operation(summary = "根据ID获取部门详情")
    @GetMapping("/{id}")
    public R<DeptVO> getDeptById(@PathVariable Long id) {
        DeptVO dept = sysDeptService.getDeptById(id);
        return R.success(dept);
    }

    @Operation(summary = "新增部门")
    @PostMapping
    public R<Void> saveDept(@Valid @RequestBody DeptSaveDTO saveDTO) {
        sysDeptService.saveDept(saveDTO);
        return R.success();
    }

    @Operation(summary = "修改部门")
    @PutMapping
    public R<Void> updateDept(@Valid @RequestBody DeptSaveDTO saveDTO) {
        sysDeptService.updateDept(saveDTO);
        return R.success();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public R<Void> deleteDept(@PathVariable Long id) {
        sysDeptService.deleteDept(id);
        return R.success();
    }
}
