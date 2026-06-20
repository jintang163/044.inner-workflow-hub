package com.innerworkflow.form.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.innerworkflow.common.result.R;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.form.dto.FormDraftQueryDTO;
import com.innerworkflow.form.dto.FormDraftSaveDTO;
import com.innerworkflow.form.service.WfFormDraftService;
import com.innerworkflow.form.vo.FormDraftVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "表单草稿管理")
@RestController
@RequestMapping("/form/draft")
@RequiredArgsConstructor
public class WfFormDraftController {

    private final WfFormDraftService formDraftService;

    @Operation(summary = "分页查询我的草稿")
    @GetMapping("/page")
    public R<Page<FormDraftVO>> page(FormDraftQueryDTO queryDTO) {
        queryDTO.setCreatorId(SecurityUtils.getCurrentUserId());
        return R.success(formDraftService.pageList(queryDTO));
    }

    @Operation(summary = "获取我的草稿列表(全部)")
    @GetMapping("/list")
    public R<List<FormDraftVO>> list(@RequestParam(required = false) String processKey) {
        return R.success(formDraftService.listMyDrafts(processKey));
    }

    @Operation(summary = "获取草稿详情")
    @GetMapping("/{id}")
    public R<FormDraftVO> getDetail(@PathVariable Long id) {
        return R.success(formDraftService.getDetail(id));
    }

    @Operation(summary = "根据草稿编号获取草稿")
    @GetMapping("/no/{draftNo}")
    public R<FormDraftVO> getByDraftNo(@PathVariable String draftNo) {
        return R.success(formDraftService.getByDraftNo(draftNo));
    }

    @Operation(summary = "获取最新草稿(按流程Key)")
    @GetMapping("/latest/{processKey}")
    public R<FormDraftVO> getLatest(@PathVariable String processKey) {
        return R.success(formDraftService.getLatestByProcessKey(processKey));
    }

    @Operation(summary = "保存草稿(手动保存)")
    @PostMapping
    public R<FormDraftVO> save(@Valid @RequestBody FormDraftSaveDTO saveDTO) {
        saveDTO.setDraftStatus(1);
        return R.success(formDraftService.saveDraft(saveDTO));
    }

    @Operation(summary = "自动保存草稿")
    @PostMapping("/auto-save")
    public R<FormDraftVO> autoSave(@RequestBody FormDraftSaveDTO saveDTO) {
        return R.success(formDraftService.autoSave(saveDTO));
    }

    @Operation(summary = "删除草稿")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.success(formDraftService.deleteDraft(id));
    }

    @Operation(summary = "清理过期草稿(管理用)")
    @DeleteMapping("/clean-expired")
    public R<Integer> cleanExpired(@RequestParam(defaultValue = "30") Integer days) {
        return R.success(formDraftService.cleanExpiredDrafts(days));
    }
}
