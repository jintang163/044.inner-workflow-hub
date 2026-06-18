package com.innerworkflow.tenant.service;

import com.innerworkflow.tenant.dto.TenantRoleSaveDTO;
import com.innerworkflow.tenant.vo.TenantRoleVO;

import java.util.List;

public interface SysTenantRoleService {

    List<TenantRoleVO> listByTenantId(Long tenantId);

    TenantRoleVO getById(Long id);

    void save(TenantRoleSaveDTO dto);

    void update(TenantRoleSaveDTO dto);

    void remove(Long id);

    void assignUserRole(Long tenantId, Long userId, Long tenantRoleId);

    void removeUserRole(Long tenantId, Long userId, Long tenantRoleId);

    List<String> getRoleCodesByTenantUser(Long tenantId, Long userId);

    List<String> getPermsByTenantUser(Long tenantId, Long userId);
}
