package com.innerworkflow.tenant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.tenant.dto.TenantQueryDTO;
import com.innerworkflow.tenant.dto.TenantRegisterDTO;
import com.innerworkflow.tenant.dto.TenantUpdateDTO;
import com.innerworkflow.tenant.dto.TenantUserQueryDTO;
import com.innerworkflow.tenant.vo.TenantStatsVO;
import com.innerworkflow.tenant.vo.TenantUserVO;
import com.innerworkflow.tenant.vo.TenantVO;

import java.util.List;

public interface SysTenantService {

    IPage<TenantVO> page(TenantQueryDTO queryDTO);

    TenantVO getById(Long id);

    void register(TenantRegisterDTO registerDTO);

    void approve(Long tenantId);

    void reject(Long tenantId);

    void update(TenantUpdateDTO updateDTO);

    void remove(Long id);

    List<TenantVO> listByUserId(Long userId);

    TenantStatsVO getStats(Long tenantId);

    IPage<TenantUserVO> pageUsers(TenantUserQueryDTO queryDTO);

    void addTenantUser(Long tenantId, Long userId, String tenantRole);

    void removeTenantUser(Long tenantId, Long userId);
}
