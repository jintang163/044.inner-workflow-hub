package com.innerworkflow.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.auth.dto.RoleQueryDTO;
import com.innerworkflow.auth.dto.RoleSaveDTO;
import com.innerworkflow.auth.entity.SysRole;
import com.innerworkflow.auth.vo.RoleVO;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    IPage<RoleVO> getRolePage(RoleQueryDTO queryDTO);

    List<RoleVO> getRoleList();

    RoleVO getRoleById(Long id);

    void saveRole(RoleSaveDTO saveDTO);

    void updateRole(RoleSaveDTO saveDTO);

    void deleteRole(Long id);

    void batchDeleteRole(List<Long> ids);

    List<SysRole> getRolesByUserId(Long userId);
}
