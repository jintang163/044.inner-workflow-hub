package com.innerworkflow.tenant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.tenant.dto.TenantQueryDTO;
import com.innerworkflow.tenant.dto.TenantRegisterDTO;
import com.innerworkflow.tenant.dto.TenantUpdateDTO;
import com.innerworkflow.tenant.dto.TenantUserQueryDTO;
import com.innerworkflow.tenant.entity.SysTenant;
import com.innerworkflow.tenant.entity.SysTenantUser;
import com.innerworkflow.tenant.mapper.SysTenantMapper;
import com.innerworkflow.tenant.mapper.SysTenantUserMapper;
import com.innerworkflow.tenant.service.SysTenantService;
import com.innerworkflow.tenant.vo.TenantStatsVO;
import com.innerworkflow.tenant.vo.TenantUserVO;
import com.innerworkflow.tenant.vo.TenantVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl implements SysTenantService {

    private final SysTenantMapper tenantMapper;
    private final SysTenantUserMapper tenantUserMapper;

    @Override
    public IPage<TenantVO> page(TenantQueryDTO queryDTO) {
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getTenantName()), SysTenant::getTenantName, queryDTO.getTenantName())
                .like(StrUtil.isNotBlank(queryDTO.getTenantCode()), SysTenant::getTenantCode, queryDTO.getTenantCode())
                .eq(queryDTO.getStatus() != null, SysTenant::getStatus, queryDTO.getStatus())
                .eq(StrUtil.isNotBlank(queryDTO.getBusinessType()), SysTenant::getBusinessType, queryDTO.getBusinessType())
                .orderByDesc(SysTenant::getCreateTime);

        IPage<SysTenant> page = tenantMapper.selectPage(queryDTO.buildPage(), wrapper);
        return page.convert(this::toTenantVO);
    }

    @Override
    public TenantVO getById(Long id) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw BusinessException.notFound("租户不存在");
        }
        return toTenantVO(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(TenantRegisterDTO registerDTO) {
        Long existing = tenantMapper.selectCount(
                new LambdaQueryWrapper<SysTenant>().eq(SysTenant::getTenantCode, registerDTO.getTenantCode()));
        if (existing > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "租户编码已存在");
        }

        SysTenant tenant = new SysTenant();
        tenant.setTenantName(registerDTO.getTenantName());
        tenant.setTenantCode(registerDTO.getTenantCode());
        tenant.setContactName(registerDTO.getContactName());
        tenant.setContactEmail(registerDTO.getContactEmail());
        tenant.setContactPhone(registerDTO.getContactPhone());
        tenant.setBusinessType(registerDTO.getBusinessType());
        tenant.setRemark(registerDTO.getRemark());
        tenant.setStatus(0);
        tenantMapper.insert(tenant);

        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (currentUserId != null) {
            SysTenantUser tenantUser = new SysTenantUser();
            tenantUser.setTenantId(tenant.getId());
            tenantUser.setUserId(currentUserId);
            tenantUser.setTenantRole("TENANT_ADMIN");
            tenantUser.setStatus(1);
            tenantUserMapper.insert(tenantUser);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long tenantId) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw BusinessException.notFound("租户不存在");
        }
        if (tenant.getStatus() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅待审核租户可审批");
        }
        tenant.setStatus(1);
        tenantMapper.updateById(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long tenantId) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw BusinessException.notFound("租户不存在");
        }
        if (tenant.getStatus() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅待审核租户可驳回");
        }
        tenant.setStatus(2);
        tenantMapper.updateById(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TenantUpdateDTO updateDTO) {
        SysTenant tenant = tenantMapper.selectById(updateDTO.getId());
        if (tenant == null) {
            throw BusinessException.notFound("租户不存在");
        }

        if (StrUtil.isNotBlank(updateDTO.getTenantName())) {
            tenant.setTenantName(updateDTO.getTenantName());
        }
        if (StrUtil.isNotBlank(updateDTO.getContactName())) {
            tenant.setContactName(updateDTO.getContactName());
        }
        if (updateDTO.getContactEmail() != null) {
            tenant.setContactEmail(updateDTO.getContactEmail());
        }
        if (updateDTO.getContactPhone() != null) {
            tenant.setContactPhone(updateDTO.getContactPhone());
        }
        if (StrUtil.isNotBlank(updateDTO.getBusinessType())) {
            tenant.setBusinessType(updateDTO.getBusinessType());
        }
        if (updateDTO.getExpireTime() != null) {
            tenant.setExpireTime(updateDTO.getExpireTime());
        }
        if (updateDTO.getRemark() != null) {
            tenant.setRemark(updateDTO.getRemark());
        }
        if (updateDTO.getStatus() != null) {
            tenant.setStatus(updateDTO.getStatus());
        }
        tenantMapper.updateById(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw BusinessException.notFound("租户不存在");
        }
        tenantMapper.deleteById(id);
        tenantUserMapper.delete(new LambdaQueryWrapper<SysTenantUser>().eq(SysTenantUser::getTenantId, id));
    }

    @Override
    public List<TenantVO> listByUserId(Long userId) {
        List<SysTenantUser> tenantUsers = tenantUserMapper.selectList(
                new LambdaQueryWrapper<SysTenantUser>().eq(SysTenantUser::getUserId, userId));
        if (tenantUsers.isEmpty()) {
            return List.of();
        }
        List<Long> tenantIds = tenantUsers.stream().map(SysTenantUser::getTenantId).collect(Collectors.toList());
        List<SysTenant> tenants = tenantMapper.selectBatchIds(tenantIds);
        return tenants.stream().map(this::toTenantVO).collect(Collectors.toList());
    }

    @Override
    public TenantStatsVO getStats(Long tenantId) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw BusinessException.notFound("租户不存在");
        }

        TenantStatsVO statsVO = new TenantStatsVO();
        statsVO.setTenantId(tenantId);
        statsVO.setTenantName(tenant.getTenantName());
        statsVO.setProcessCount(tenantMapper.countProcessByTenantId(tenantId));
        statsVO.setPendingCount(tenantMapper.countPendingByTenantId(tenantId));
        statsVO.setAvgDuration(tenantMapper.avgDurationByTenantId(tenantId));
        return statsVO;
    }

    @Override
    public IPage<TenantUserVO> pageUsers(TenantUserQueryDTO queryDTO) {
        LambdaQueryWrapper<SysTenantUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryDTO.getTenantId() != null, SysTenantUser::getTenantId, queryDTO.getTenantId())
                .eq(StrUtil.isNotBlank(queryDTO.getTenantRole()), SysTenantUser::getTenantRole, queryDTO.getTenantRole());

        IPage<SysTenantUser> page = tenantUserMapper.selectPage(queryDTO.buildPage(), wrapper);
        return page.convert(this::toTenantUserVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTenantUser(Long tenantId, Long userId, String tenantRole) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw BusinessException.notFound("租户不存在");
        }

        Long existing = tenantUserMapper.selectCount(
                new LambdaQueryWrapper<SysTenantUser>()
                        .eq(SysTenantUser::getTenantId, tenantId)
                        .eq(SysTenantUser::getUserId, userId));
        if (existing > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户已在该租户中");
        }

        SysTenantUser tenantUser = new SysTenantUser();
        tenantUser.setTenantId(tenantId);
        tenantUser.setUserId(userId);
        tenantUser.setTenantRole(tenantRole);
        tenantUser.setStatus(1);
        tenantUserMapper.insert(tenantUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTenantUser(Long tenantId, Long userId) {
        tenantUserMapper.delete(
                new LambdaQueryWrapper<SysTenantUser>()
                        .eq(SysTenantUser::getTenantId, tenantId)
                        .eq(SysTenantUser::getUserId, userId));
    }

    private TenantVO toTenantVO(SysTenant tenant) {
        TenantVO vo = new TenantVO();
        vo.setId(tenant.getId());
        vo.setTenantName(tenant.getTenantName());
        vo.setTenantCode(tenant.getTenantCode());
        vo.setContactName(tenant.getContactName());
        vo.setContactEmail(tenant.getContactEmail());
        vo.setContactPhone(tenant.getContactPhone());
        vo.setBusinessType(tenant.getBusinessType());
        vo.setStatus(tenant.getStatus());
        vo.setExpireTime(tenant.getExpireTime());
        vo.setRemark(tenant.getRemark());
        vo.setCreateTime(tenant.getCreateTime());

        Long userCount = tenantUserMapper.selectCount(
                new LambdaQueryWrapper<SysTenantUser>().eq(SysTenantUser::getTenantId, tenant.getId()));
        vo.setUserCount(userCount);
        return vo;
    }

    private TenantUserVO toTenantUserVO(SysTenantUser tenantUser) {
        TenantUserVO vo = new TenantUserVO();
        vo.setId(tenantUser.getId());
        vo.setTenantId(tenantUser.getTenantId());
        vo.setUserId(tenantUser.getUserId());
        vo.setTenantRole(tenantUser.getTenantRole());
        vo.setStatus(tenantUser.getStatus());
        return vo;
    }
}
