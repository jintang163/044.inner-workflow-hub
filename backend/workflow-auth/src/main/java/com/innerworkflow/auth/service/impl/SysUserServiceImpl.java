package com.innerworkflow.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.auth.dto.UserQueryDTO;
import com.innerworkflow.auth.dto.UserSaveDTO;
import com.innerworkflow.auth.entity.SysRole;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.entity.SysUserRole;
import com.innerworkflow.auth.entity.SysDepartment;
import com.innerworkflow.auth.mapper.SysDeptMapper;
import com.innerworkflow.auth.mapper.SysMenuMapper;
import com.innerworkflow.auth.mapper.SysUserMapper;
import com.innerworkflow.auth.mapper.SysUserRoleMapper;
import com.innerworkflow.auth.service.SysRoleService;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.auth.vo.UserVO;
import com.innerworkflow.common.dto.LoginUserDTO;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.tenant.entity.SysTenant;
import com.innerworkflow.tenant.entity.SysTenantUser;
import com.innerworkflow.tenant.mapper.SysTenantMapper;
import com.innerworkflow.tenant.mapper.SysTenantUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleService sysRoleService;
    private final SysDeptMapper sysDeptMapper;
    private final SysMenuMapper sysMenuMapper;
    private final PasswordEncoder passwordEncoder;
    private final SysTenantUserMapper sysTenantUserMapper;
    private final SysTenantMapper sysTenantMapper;

    @Override
    public IPage<UserVO> getUserPage(UserQueryDTO queryDTO) {
        Page<SysUser> page = queryDTO.buildPage("create_time desc");
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getUsername()), SysUser::getUsername, queryDTO.getUsername())
                .like(StrUtil.isNotBlank(queryDTO.getRealName()), SysUser::getRealName, queryDTO.getRealName())
                .like(StrUtil.isNotBlank(queryDTO.getPhone()), SysUser::getPhone, queryDTO.getPhone())
                .eq(queryDTO.getStatus() != null, SysUser::getStatus, queryDTO.getStatus())
                .eq(queryDTO.getDeptId() != null, SysUser::getDeptId, queryDTO.getDeptId());

        IPage<SysUser> userPage = this.page(page, wrapper);
        List<UserVO> userVOList = userPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<UserVO> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(userVOList);
        return result;
    }

    @Override
    public UserVO getUserById(Long id) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return convertToVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUser(UserSaveDTO saveDTO) {
        SysUser existsUser = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, saveDTO.getUsername()));
        if (existsUser != null) {
            throw BusinessException.paramError("用户名已存在");
        }

        SysUser user = BeanUtil.copyProperties(saveDTO, SysUser.class);
        if (StrUtil.isNotBlank(saveDTO.getPassword())) {
            user.setPassword(passwordEncoder.encode(saveDTO.getPassword()));
        }
        this.save(user);

        if (saveDTO.getRoleIds() != null && !saveDTO.getRoleIds().isEmpty()) {
            saveUserRoles(user.getId(), saveDTO.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserSaveDTO saveDTO) {
        SysUser user = this.getById(saveDTO.getId());
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        if (!user.getUsername().equals(saveDTO.getUsername())) {
            SysUser existsUser = this.getOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, saveDTO.getUsername()));
            if (existsUser != null) {
                throw BusinessException.paramError("用户名已存在");
            }
        }

        BeanUtil.copyProperties(saveDTO, user);
        if (StrUtil.isNotBlank(saveDTO.getPassword())) {
            user.setPassword(passwordEncoder.encode(saveDTO.getPassword()));
        }
        this.updateById(user);

        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, user.getId()));

        if (saveDTO.getRoleIds() != null && !saveDTO.getRoleIds().isEmpty()) {
            saveUserRoles(user.getId(), saveDTO.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        this.removeById(id);
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteUser(List<Long> ids) {
        this.removeByIds(ids);
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getUserId, ids));
    }

    @Override
    public LoginUserDTO getLoginUserByUsername(String username) {
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            return null;
        }

        Set<String> roles = new HashSet<>(getRolesByUserId(user.getId()));
        Set<String> permissions = new HashSet<>(getPermissionsByUserId(user.getId()));

        String deptName = null;
        if (user.getDeptId() != null) {
            SysDepartment dept = sysDeptMapper.selectById(user.getDeptId());
            if (dept != null) {
                deptName = dept.getDeptName();
            }
        }

        List<SysTenantUser> tenantUsers = sysTenantUserMapper.selectList(
                new LambdaQueryWrapper<SysTenantUser>()
                        .eq(SysTenantUser::getUserId, user.getId())
                        .eq(SysTenantUser::getStatus, 1)
        );

        Set<Long> tenantIds = tenantUsers.stream()
                .map(SysTenantUser::getTenantId)
                .collect(Collectors.toSet());

        Long defaultTenantId = null;
        if (!tenantIds.isEmpty()) {
            List<SysTenant> tenants = sysTenantMapper.selectList(
                    new LambdaQueryWrapper<SysTenant>()
                            .in(SysTenant::getId, tenantIds)
                            .eq(SysTenant::getStatus, 1)
                            .orderByAsc(SysTenant::getId)
            );
            if (!tenants.isEmpty()) {
                defaultTenantId = tenants.get(0).getId();
            }
        }

        return LoginUserDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .deptId(user.getDeptId())
                .deptName(deptName)
                .roles(roles)
                .permissions(permissions)
                .tenantId(defaultTenantId)
                .tenantIds(tenantIds)
                .build();
    }

    @Override
    public List<String> getRolesByUserId(Long userId) {
        List<SysRole> roles = sysRoleService.getRolesByUserId(userId);
        return roles.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        return sysMenuMapper.selectPermsByUserId(userId);
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        List<SysUserRole> userRoleList = roleIds.stream()
                .map(roleId -> {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .collect(Collectors.toList());
        userRoleList.forEach(sysUserRoleMapper::insert);
    }

    private UserVO convertToVO(SysUser user) {
        UserVO vo = BeanUtil.copyProperties(user, UserVO.class);
        vo.setId(user.getId());

        if (user.getDeptId() != null) {
            SysDepartment dept = sysDeptMapper.selectById(user.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }

        List<SysRole> roles = sysRoleService.getRolesByUserId(user.getId());
        vo.setRoleIds(roles.stream().map(SysRole::getId).collect(Collectors.toList()));
        vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));

        return vo;
    }
}
