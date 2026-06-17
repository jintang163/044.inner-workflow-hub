package com.innerworkflow.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.auth.dto.RoleQueryDTO;
import com.innerworkflow.auth.dto.RoleSaveDTO;
import com.innerworkflow.auth.entity.SysRole;
import com.innerworkflow.auth.entity.SysRoleMenu;
import com.innerworkflow.auth.entity.SysRoleDept;
import com.innerworkflow.auth.entity.SysUserRole;
import com.innerworkflow.auth.mapper.SysRoleMapper;
import com.innerworkflow.auth.mapper.SysRoleMenuMapper;
import com.innerworkflow.auth.mapper.SysRoleDeptMapper;
import com.innerworkflow.auth.mapper.SysUserRoleMapper;
import com.innerworkflow.auth.service.SysRoleService;
import com.innerworkflow.auth.vo.RoleVO;
import com.innerworkflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysRoleDeptMapper sysRoleDeptMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public IPage<RoleVO> getRolePage(RoleQueryDTO queryDTO) {
        Page<SysRole> page = queryDTO.buildPage("role_sort asc");
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getRoleName()), SysRole::getRoleName, queryDTO.getRoleName())
                .like(StrUtil.isNotBlank(queryDTO.getRoleCode()), SysRole::getRoleCode, queryDTO.getRoleCode())
                .eq(queryDTO.getStatus() != null, SysRole::getStatus, queryDTO.getStatus());

        IPage<SysRole> rolePage = this.page(page, wrapper);
        List<RoleVO> roleVOList = rolePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<RoleVO> result = new Page<>(rolePage.getCurrent(), rolePage.getSize(), rolePage.getTotal());
        result.setRecords(roleVOList);
        return result;
    }

    @Override
    public List<RoleVO> getRoleList() {
        List<SysRole> list = this.list(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getRoleSort));
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public RoleVO getRoleById(Long id) {
        SysRole role = this.getById(id);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        return convertToVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRole(RoleSaveDTO saveDTO) {
        SysRole existsRole = this.getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, saveDTO.getRoleCode()));
        if (existsRole != null) {
            throw BusinessException.paramError("角色编码已存在");
        }

        SysRole role = BeanUtil.copyProperties(saveDTO, SysRole.class);
        this.save(role);

        saveRoleMenus(role.getId(), saveDTO.getMenuIds());
        saveRoleDepts(role.getId(), saveDTO.getDeptIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleSaveDTO saveDTO) {
        SysRole role = this.getById(saveDTO.getId());
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }

        if (!role.getRoleCode().equals(saveDTO.getRoleCode())) {
            SysRole existsRole = this.getOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, saveDTO.getRoleCode()));
            if (existsRole != null) {
                throw BusinessException.paramError("角色编码已存在");
            }
        }

        BeanUtil.copyProperties(saveDTO, role);
        this.updateById(role);

        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, role.getId()));
        sysRoleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getRoleId, role.getId()));

        saveRoleMenus(role.getId(), saveDTO.getMenuIds());
        saveRoleDepts(role.getId(), saveDTO.getDeptIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        long count = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (count > 0) {
            throw BusinessException.paramError("该角色下存在用户，不允许删除");
        }

        this.removeById(id);
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, id));
        sysRoleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getRoleId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteRole(List<Long> ids) {
        long count = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getRoleId, ids));
        if (count > 0) {
            throw BusinessException.paramError("所选角色下存在用户，不允许删除");
        }

        this.removeByIds(ids);
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getRoleId, ids));
        sysRoleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>()
                .in(SysRoleDept::getRoleId, ids));
    }

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        return this.list(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1));
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<SysRoleMenu> roleMenuList = menuIds.stream()
                .map(menuId -> {
                    SysRoleMenu roleMenu = new SysRoleMenu();
                    roleMenu.setRoleId(roleId);
                    roleMenu.setMenuId(menuId);
                    return roleMenu;
                })
                .collect(Collectors.toList());
        roleMenuList.forEach(sysRoleMenuMapper::insert);
    }

    private void saveRoleDepts(Long roleId, List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return;
        }
        List<SysRoleDept> roleDeptList = deptIds.stream()
                .map(deptId -> {
                    SysRoleDept roleDept = new SysRoleDept();
                    roleDept.setRoleId(roleId);
                    roleDept.setDeptId(deptId);
                    return roleDept;
                })
                .collect(Collectors.toList());
        roleDeptList.forEach(sysRoleDeptMapper::insert);
    }

    private RoleVO convertToVO(SysRole role) {
        RoleVO vo = BeanUtil.copyProperties(role, RoleVO.class);

        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, role.getId()));
        vo.setMenuIds(roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList()));

        List<SysRoleDept> roleDepts = sysRoleDeptMapper.selectList(new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getRoleId, role.getId()));
        vo.setDeptIds(roleDepts.stream().map(SysRoleDept::getDeptId).collect(Collectors.toList()));

        return vo;
    }
}
