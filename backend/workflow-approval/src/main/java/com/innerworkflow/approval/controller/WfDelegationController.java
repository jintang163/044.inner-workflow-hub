package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfDelegationQueryDTO;
import com.innerworkflow.approval.dto.WfDelegationSaveDTO;
import com.innerworkflow.approval.service.WfDelegationService;
import com.innerworkflow.approval.vo.WfDelegationVO;
import com.innerworkflow.common.result.R;
import com.innerworkflow.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "委托代理", description = "委托代理相关接口")
@RestController
@RequestMapping("/approval/delegation")
@RequiredArgsConstructor
public class WfDelegationController {

    private final WfDelegationService delegationService;

    @Operation(summary = "分页查询委托列表")
    @GetMapping("/page")
    public R<IPage<WfDelegationVO>> page(WfDelegationQueryDTO queryDTO) {
        return R.success(delegationService.page(queryDTO));
    }

    @Operation(summary = "获取委托详情")
    @GetMapping("/{id}")
    public R<WfDelegationVO> getById(@PathVariable Long id) {
        return R.success(delegationService.getById(id));
    }

    @Operation(summary = "创建委托")
    @PostMapping
    public R<Void> save(@Valid @RequestBody WfDelegationSaveDTO dto) {
        delegationService.saveDelegation(dto);
        return R.success();
    }

    @Operation(summary = "更新委托")
    @PutMapping
    public R<Void> update(@Valid @RequestBody WfDelegationSaveDTO dto) {
        delegationService.updateDelegation(dto);
        return R.success();
    }

    @Operation(summary = "撤销委托")
    @PutMapping("/{id}/revoke")
    public R<Void> revoke(@PathVariable Long id) {
        delegationService.revokeDelegation(id);
        return R.success();
    }

    @Operation(summary = "获取当前用户是否有生效的委托")
    @GetMapping("/active/current")
    public R<Boolean> hasActiveDelegation() {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean hasActive = !delegationService.listActiveDelegationsByDelegatorId(userId).isEmpty();
        return R.success(hasActive);
    }
}
