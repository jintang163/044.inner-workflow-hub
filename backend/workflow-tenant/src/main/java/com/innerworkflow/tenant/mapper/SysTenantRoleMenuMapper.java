package com.innerworkflow.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.innerworkflow.tenant.entity.SysTenantRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysTenantRoleMenuMapper extends BaseMapper<SysTenantRoleMenu> {

    @Select("SELECT DISTINCT m.permission FROM sys_menu m INNER JOIN sys_tenant_role_menu trm ON m.id = trm.menu_id INNER JOIN sys_tenant_user_role tur ON trm.tenant_role_id = tur.tenant_role_id WHERE tur.tenant_id = #{tenantId} AND tur.user_id = #{userId} AND m.status = 1 AND m.is_deleted = 0 AND m.permission IS NOT NULL AND m.permission != ''")
    List<String> selectPermsByTenantUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Select("SELECT DISTINCT r.role_code FROM sys_tenant_role r INNER JOIN sys_tenant_user_role tur ON r.id = tur.tenant_role_id WHERE tur.tenant_id = #{tenantId} AND tur.user_id = #{userId} AND r.status = 1 AND r.is_deleted = 0")
    List<String> selectRoleCodesByTenantUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
