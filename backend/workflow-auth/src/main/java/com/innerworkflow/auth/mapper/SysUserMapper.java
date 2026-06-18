package com.innerworkflow.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.innerworkflow.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT u.* FROM sys_user u WHERE u.dept_id = #{deptId} AND u.is_deleted = 0 AND u.status = 1")
    List<SysUser> selectByDeptId(@Param("deptId") Long deptId);

    @Select("SELECT u.* FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON u.id = ur.user_id " +
            "WHERE ur.role_id = #{roleId} AND u.is_deleted = 0 AND u.status = 1")
    List<SysUser> selectByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT u.* FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON u.id = ur.user_id " +
            "INNER JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE r.role_code = #{roleCode} AND u.is_deleted = 0 AND u.status = 1")
    List<SysUser> selectByRoleCode(@Param("roleCode") String roleCode);
}
