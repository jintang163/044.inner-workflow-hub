package com.innerworkflow.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.innerworkflow.tenant.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {

    @Select("SELECT COUNT(*) FROM wf_process_instance WHERE tenant_id = #{tenantId} AND is_deleted = 0")
    Long countProcessByTenantId(@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM wf_approval_task WHERE task_status = 0 AND tenant_id = #{tenantId}")
    Long countPendingByTenantId(@Param("tenantId") Long tenantId);

    @Select("SELECT COALESCE(CAST(AVG(duration) AS SIGNED), 0) FROM wf_process_instance WHERE tenant_id = #{tenantId} AND is_deleted = 0 AND duration IS NOT NULL")
    Long avgDurationByTenantId(@Param("tenantId") Long tenantId);
}
