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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "表单草稿管理")
@RestController
@RequestMapping("/form/draft")
public class WfFormDraftController {

    @Autowired
    private WfFormDraftService formDraftService;

    @Operation(summary = "分页查询我的草稿")
    @GetMapping("/page")
    public R<Page<FormDraftVO>> page(FormDraftQueryDTO queryDTO) {
        queryDTO.setCreatorId(SecurityUtils.getCurrentUserId());
        return R.success(formDraftService.pageList(queryDTO));
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

    @Operation(summary = "保存草稿")
    @PostMapping
    public R<FormDraftVO> save(@Valid @RequestBody FormDraftSaveDTO saveDTO) {
        return R.success(formDraftService.saveDraft(saveDTO));
    }

    @Operation(summary = "删除草稿")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.success(formDraftService.deleteDraft(id));
    }
}
