package com.innerworkflow.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.auth.dto.UserQueryDTO;
import com.innerworkflow.auth.dto.UserSaveDTO;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.vo.UserVO;
import com.innerworkflow.common.dto.LoginUserDTO;

import java.util.List;

public interface SysUserService extends IService<SysUser> {

    IPage<UserVO> getUserPage(UserQueryDTO queryDTO);

    UserVO getUserById(Long id);

    void saveUser(UserSaveDTO saveDTO);

    void updateUser(UserSaveDTO saveDTO);

    void deleteUser(Long id);

    void batchDeleteUser(List<Long> ids);

    LoginUserDTO getLoginUserByUsername(String username);

    List<String> getRolesByUserId(Long userId);

    List<String> getPermissionsByUserId(Long userId);

    List<SysUser> listByDeptId(Long deptId);

    List<SysUser> listByRoleId(Long roleId);

    List<SysUser> listByRoleCode(String roleCode);
}
