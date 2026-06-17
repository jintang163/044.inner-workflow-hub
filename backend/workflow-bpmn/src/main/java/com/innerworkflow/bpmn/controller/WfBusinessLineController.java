package com.innerworkflow.bpmn.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.bpmn.dto.WfBusinessLineQueryDTO;
import com.innerworkflow.bpmn.entity.WfBusinessLine;
import com.innerworkflow.bpmn.service.WfBusinessLineService;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "业务线管理", description = "业务线相关接口")
@RestController
@RequestMapping("/bpmn/business-line")
@RequiredArgsConstructor
public class WfBusinessLineController {

    private final WfBusinessLineService businessLineService;

    @Operation(summary = "分页查询业务线")
    @GetMapping("/page")
    public R<IPage<WfBusinessLine>> page(WfBusinessLineQueryDTO queryDTO) {
        return R.success(businessLineService.page(queryDTO));
    }

    @Operation(summary = "获取所有启用的业务线列表")
    @GetMapping("/list")
    public R<List<WfBusinessLine>> list() {
        return R.success(businessLineService.listAll());
    }

    @Operation(summary = "根据ID获取业务线详情")
    @GetMapping("/{id}")
    public R<WfBusinessLine> getById(@PathVariable Long id) {
        return R.success(businessLineService.getById(id));
    }

    @Operation(summary = "新增业务线")
    @PostMapping
    public R<Void> save(@RequestBody WfBusinessLine businessLine) {
        businessLineService.save(businessLine);
        return R.success();
    }

    @Operation(summary = "修改业务线")
    @PutMapping
    public R<Void> update(@RequestBody WfBusinessLine businessLine) {
        businessLineService.updateById(businessLine);
        return R.success();
    }

    @Operation(summary = "删除业务线")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        businessLineService.removeById(id);
        return R.success();
    }
}
