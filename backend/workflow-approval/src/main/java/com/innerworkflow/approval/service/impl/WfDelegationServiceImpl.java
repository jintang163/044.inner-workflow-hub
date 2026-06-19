package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfDelegationQueryDTO;
import com.innerworkflow.approval.dto.WfDelegationSaveDTO;
import com.innerworkflow.approval.entity.WfDelegation;
import com.innerworkflow.approval.mapper.WfDelegationMapper;
import com.innerworkflow.approval.service.WfDelegationService;
import com.innerworkflow.approval.service.WfTransferRecordService;
import com.innerworkflow.approval.vo.WfDelegationVO;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.common.enums.DelegationStatusEnum;
import com.innerworkflow.common.enums.TransferTypeEnum;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.result.ResultCode;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.enums.EventTypeEnum;
import com.innerworkflow.notify.service.WfNotifyService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfDelegationServiceImpl extends ServiceImpl<WfDelegationMapper, WfDelegation> implements WfDelegationService {

    private final SysUserService sysUserService;
    private final WfNotifyService notifyService;
    private final WfTransferRecordService transferRecordService;

    @Override
    public IPage<WfDelegationVO> page(WfDelegationQueryDTO queryDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<WfDelegation> wrapper = new LambdaQueryWrapper<>();

        Integer queryType = queryDTO.getQueryType();
        if (queryType != null && queryType == 1) {
            wrapper.eq(WfDelegation::getDelegatorId, userId);
        } else if (queryType != null && queryType == 2) {
            wrapper.eq(WfDelegation::getDelegateeId, userId);
        } else {
            wrapper.and(w -> w.eq(WfDelegation::getDelegatorId, userId)
                    .or().eq(WfDelegation::getDelegateeId, userId));
        }

        if (queryDTO.getDelegationStatus() != null) {
            wrapper.eq(WfDelegation::getDelegationStatus, queryDTO.getDelegationStatus());
        }

        if (StrUtil.isNotBlank(queryDTO.getDelegatorName())) {
            wrapper.like(WfDelegation::getDelegatorName, queryDTO.getDelegatorName());
        }

        if (StrUtil.isNotBlank(queryDTO.getDelegateeName())) {
            wrapper.like(WfDelegation::getDelegateeName, queryDTO.getDelegateeName());
        }

        wrapper.orderByDesc(WfDelegation::getCreateTime);

        return this.page(queryDTO.buildPage("create_time desc"), wrapper)
                .convert(this::convertToVO);
    }

    @Override
    public WfDelegationVO getById(Long id) {
        WfDelegation delegation = super.getById(id);
        if (delegation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "委托记录不存在");
        }
        return convertToVO(delegation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDelegation(WfDelegationSaveDTO dto) {
        validateDelegationTime(dto.getStartTime(), dto.getEndTime());

        Long userId = SecurityUtils.getCurrentUserId();
        SysUser currentUser = sysUserService.getById(userId);

        if (userId.equals(dto.getDelegateeId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能委托给自己");
        }

        SysUser delegatee = sysUserService.getById(dto.getDelegateeId());
        if (delegatee == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "代理人不存在");
        }

        WfDelegation delegation = new WfDelegation();
        BeanUtils.copyProperties(dto, delegation);
        delegation.setDelegatorId(userId);
        delegation.setDelegatorName(currentUser != null ? currentUser.getRealName() : "");
        delegation.setDelegateeName(delegatee.getRealName());

        if (dto.getProcessKeys() != null && !dto.getProcessKeys().isEmpty()) {
            delegation.setProcessKeys(String.join(",", dto.getProcessKeys()));
        }

        LocalDateTime now = LocalDateTime.now();
        if (dto.getStartTime().isBefore(now) || dto.getStartTime().isEqual(now)) {
            delegation.setDelegationStatus(DelegationStatusEnum.ACTIVE.getCode());
        } else {
            delegation.setDelegationStatus(DelegationStatusEnum.PENDING.getCode());
        }

        this.save(delegation);

        if (DelegationStatusEnum.ACTIVE.getCode().equals(delegation.getDelegationStatus())) {
            sendDelegationStartNotify(delegation);
        }

        log.info("创建委托成功, delegatorId={}, delegateeId={}, startTime={}, endTime={}",
                userId, dto.getDelegateeId(), dto.getStartTime(), dto.getEndTime());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDelegation(WfDelegationSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "委托ID不能为空");
        }

        WfDelegation existing = this.getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "委托记录不存在");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (!userId.equals(existing.getDelegatorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有委托人可以修改委托");
        }

        if (DelegationStatusEnum.REVOKED.getCode().equals(existing.getDelegationStatus())
                || DelegationStatusEnum.EXPIRED.getCode().equals(existing.getDelegationStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "委托已撤销或已过期，无法修改");
        }

        validateDelegationTime(dto.getStartTime(), dto.getEndTime());

        if (userId.equals(dto.getDelegateeId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能委托给自己");
        }

        SysUser delegatee = sysUserService.getById(dto.getDelegateeId());
        if (delegatee == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "代理人不存在");
        }

        WfDelegation delegation = new WfDelegation();
        BeanUtils.copyProperties(dto, delegation);
        delegation.setDelegateeName(delegatee.getRealName());

        if (dto.getProcessKeys() != null && !dto.getProcessKeys().isEmpty()) {
            delegation.setProcessKeys(String.join(",", dto.getProcessKeys()));
        } else {
            delegation.setProcessKeys(null);
        }

        LocalDateTime now = LocalDateTime.now();
        Integer oldStatus = existing.getDelegationStatus();
        if (dto.getStartTime().isBefore(now) || dto.getStartTime().isEqual(now)) {
            delegation.setDelegationStatus(DelegationStatusEnum.ACTIVE.getCode());
        } else {
            delegation.setDelegationStatus(DelegationStatusEnum.PENDING.getCode());
        }

        this.updateById(delegation);

        if (!DelegationStatusEnum.ACTIVE.getCode().equals(oldStatus)
                && DelegationStatusEnum.ACTIVE.getCode().equals(delegation.getDelegationStatus())) {
            sendDelegationStartNotify(delegation);
        }

        log.info("更新委托成功, id={}, delegatorId={}, delegateeId={}", dto.getId(), userId, dto.getDelegateeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeDelegation(Long id) {
        WfDelegation delegation = this.getById(id);
        if (delegation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "委托记录不存在");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (!userId.equals(delegation.getDelegatorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有委托人可以撤销委托");
        }

        if (DelegationStatusEnum.REVOKED.getCode().equals(delegation.getDelegationStatus())
                || DelegationStatusEnum.EXPIRED.getCode().equals(delegation.getDelegationStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "委托已撤销或已过期");
        }

        delegation.setDelegationStatus(DelegationStatusEnum.REVOKED.getCode());
        this.updateById(delegation);

        log.info("撤销委托成功, id={}, delegatorId={}", id, userId);
    }

    @Override
    public List<WfDelegation> listActiveDelegationsByDelegatorId(Long delegatorId) {
        LambdaQueryWrapper<WfDelegation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDelegation::getDelegatorId, delegatorId);
        wrapper.eq(WfDelegation::getDelegationStatus, DelegationStatusEnum.ACTIVE.getCode());
        wrapper.orderByDesc(WfDelegation::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public WfDelegation getActiveDelegation(Long delegatorId, String processKey) {
        List<WfDelegation> delegations = listActiveDelegationsByDelegatorId(delegatorId);
        if (delegations == null || delegations.isEmpty()) {
            return null;
        }

        for (WfDelegation delegation : delegations) {
            String processKeys = delegation.getProcessKeys();
            if (StrUtil.isBlank(processKeys)) {
                return delegation;
            }
            List<String> processKeyList = Arrays.asList(processKeys.split(","));
            if (processKeyList.contains(processKey)) {
                return delegation;
            }
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processDelegationStatus() {
        LocalDateTime now = LocalDateTime.now();

        List<WfDelegation> pendingStart = listPendingStartDelegations();
        for (WfDelegation delegation : pendingStart) {
            try {
                delegation.setDelegationStatus(DelegationStatusEnum.ACTIVE.getCode());
                this.updateById(delegation);
                sendDelegationStartNotify(delegation);
                log.info("委托自动生效, id={}, delegatorId={}, delegateeId={}",
                        delegation.getId(), delegation.getDelegatorId(), delegation.getDelegateeId());
            } catch (Exception e) {
                log.error("委托生效处理失败, id={}, error={}", delegation.getId(), e.getMessage(), e);
            }
        }

        List<WfDelegation> pendingEnd = listPendingEndDelegations();
        for (WfDelegation delegation : pendingEnd) {
            try {
                delegation.setDelegationStatus(DelegationStatusEnum.EXPIRED.getCode());
                this.updateById(delegation);
                sendDelegationEndNotify(delegation);
                log.info("委托自动过期, id={}, delegatorId={}, delegateeId={}",
                        delegation.getId(), delegation.getDelegatorId(), delegation.getDelegateeId());
            } catch (Exception e) {
                log.error("委托过期处理失败, id={}, error={}", delegation.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public List<WfDelegation> listPendingStartDelegations() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<WfDelegation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDelegation::getDelegationStatus, DelegationStatusEnum.PENDING.getCode());
        wrapper.le(WfDelegation::getStartTime, now);
        return this.list(wrapper);
    }

    @Override
    public List<WfDelegation> listPendingEndDelegations() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<WfDelegation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDelegation::getDelegationStatus, DelegationStatusEnum.ACTIVE.getCode());
        wrapper.lt(WfDelegation::getEndTime, now);
        return this.list(wrapper);
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        WfDelegation delegation = new WfDelegation();
        delegation.setId(id);
        delegation.setDelegationStatus(status);
        return this.updateById(delegation);
    }

    private void validateDelegationTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "委托时间不能为空");
        }
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
    }

    private WfDelegationVO convertToVO(WfDelegation delegation) {
        WfDelegationVO vo = new WfDelegationVO();
        BeanUtils.copyProperties(delegation, vo);

        DelegationStatusEnum statusEnum = DelegationStatusEnum.getByCode(delegation.getDelegationStatus());
        if (statusEnum != null) {
            vo.setDelegationStatusName(statusEnum.getDesc());
        }

        if (StrUtil.isNotBlank(delegation.getProcessKeys())) {
            vo.setProcessKeyList(Arrays.asList(delegation.getProcessKeys().split(",")));
        } else {
            vo.setProcessKeyList(Collections.emptyList());
        }

        return vo;
    }

    private void sendDelegationStartNotify(WfDelegation delegation) {
        try {
            NotifySendDTO sendDTO = new NotifySendDTO();
            sendDTO.setEventType("DELEGATION_START");
            sendDTO.setBusinessType("WORKFLOW");
            sendDTO.setReceiverUserId(delegation.getDelegateeId());

            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("delegatorUserName", delegation.getDelegatorName());
            params.put("delegateeUserName", delegation.getDelegateeName());
            params.put("startTime", delegation.getStartTime());
            params.put("endTime", delegation.getEndTime());
            params.put("delegationReason", delegation.getDelegationReason());
            params.put("delegationId", delegation.getId());
            sendDTO.setParams(params);

            notifyService.sendNotify(sendDTO);
        } catch (Exception e) {
            log.warn("发送委托生效通知失败, delegationId={}, error={}", delegation.getId(), e.getMessage());
        }
    }

    private void sendDelegationEndNotify(WfDelegation delegation) {
        try {
            NotifySendDTO sendDTO = new NotifySendDTO();
            sendDTO.setEventType("DELEGATION_END");
            sendDTO.setBusinessType("WORKFLOW");
            sendDTO.setReceiverUserId(delegation.getDelegatorId());

            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("delegatorUserName", delegation.getDelegatorName());
            params.put("delegateeUserName", delegation.getDelegateeName());
            params.put("startTime", delegation.getStartTime());
            params.put("endTime", delegation.getEndTime());
            params.put("delegationId", delegation.getId());
            sendDTO.setParams(params);

            notifyService.sendNotify(sendDTO);
        } catch (Exception e) {
            log.warn("发送委托到期通知失败, delegationId={}, error={}", delegation.getId(), e.getMessage());
        }
    }
}
