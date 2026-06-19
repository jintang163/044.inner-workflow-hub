package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.entity.WfTransferRecord;
import com.innerworkflow.approval.mapper.WfTransferRecordMapper;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.service.WfTransferRecordService;
import com.innerworkflow.approval.vo.WfTransferRecordVO;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.common.dto.PageQuery;
import com.innerworkflow.common.enums.TransferTypeEnum;
import com.innerworkflow.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfTransferRecordServiceImpl extends ServiceImpl<WfTransferRecordMapper, WfTransferRecord> implements WfTransferRecordService {

    private final WfProcessInstanceService processInstanceService;
    private final SysUserService sysUserService;

    @Override
    public IPage<WfTransferRecordVO> page(PageQuery queryDTO, Long userId, Integer transferType) {
        LambdaQueryWrapper<WfTransferRecord> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.and(w -> w.eq(WfTransferRecord::getSourceUserId, userId)
                    .or().eq(WfTransferRecord::getTargetUserId, userId));
        }

        if (transferType != null) {
            wrapper.eq(WfTransferRecord::getTransferType, transferType);
        }

        wrapper.orderByDesc(WfTransferRecord::getCreateTime);

        return this.page(queryDTO.buildPage("create_time desc"), wrapper)
                .convert(this::convertToVO);
    }

    @Override
    public List<WfTransferRecord> listByTaskId(Long taskId) {
        LambdaQueryWrapper<WfTransferRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTransferRecord::getTaskId, taskId);
        wrapper.orderByDesc(WfTransferRecord::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<WfTransferRecord> listByDelegationId(Long delegationId) {
        LambdaQueryWrapper<WfTransferRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTransferRecord::getDelegationId, delegationId);
        wrapper.eq(WfTransferRecord::getTransferType, TransferTypeEnum.DELEGATION.getCode());
        wrapper.orderByDesc(WfTransferRecord::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<WfTransferRecordVO> listVOByInstanceId(Long instanceId) {
        LambdaQueryWrapper<WfTransferRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTransferRecord::getInstanceId, instanceId);
        wrapper.orderByDesc(WfTransferRecord::getCreateTime);
        return this.list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public void createTransferRecord(Long instanceId, Long taskId, Integer transferType,
                                     Long sourceUserId, String sourceUserName,
                                     Long targetUserId, String targetUserName,
                                     String transferReason, Long delegationId) {
        WfTransferRecord record = new WfTransferRecord();
        record.setInstanceId(instanceId);
        record.setTaskId(taskId);
        record.setTransferType(transferType);
        record.setSourceUserId(sourceUserId);
        record.setSourceUserName(sourceUserName);
        record.setTargetUserId(targetUserId);
        record.setTargetUserName(targetUserName);
        record.setTransferReason(transferReason);
        record.setDelegationId(delegationId);
        record.setCreateTime(LocalDateTime.now());

        if (instanceId != null) {
            WfProcessInstance instance = processInstanceService.getById(instanceId);
            if (instance != null) {
                record.setInstanceNo(instance.getInstanceNo());
            }
        }

        if (sourceUserId != null && sourceUserName == null) {
            SysUser user = sysUserService.getById(sourceUserId);
            if (user != null) {
                record.setSourceUserName(user.getRealName());
            }
        }

        if (targetUserId != null && targetUserName == null) {
            SysUser user = sysUserService.getById(targetUserId);
            if (user != null) {
                record.setTargetUserName(user.getRealName());
            }
        }

        this.save(record);
    }

    private WfTransferRecordVO convertToVO(WfTransferRecord record) {
        WfTransferRecordVO vo = new WfTransferRecordVO();
        BeanUtils.copyProperties(record, vo);

        TransferTypeEnum typeEnum = TransferTypeEnum.getByCode(record.getTransferType());
        if (typeEnum != null) {
            vo.setTransferTypeName(typeEnum.getDesc());
        }

        if (record.getInstanceId() != null) {
            WfProcessInstance instance = processInstanceService.getById(record.getInstanceId());
            if (instance != null) {
                vo.setTitle(instance.getTitle());
            }
        }

        return vo;
    }
}
