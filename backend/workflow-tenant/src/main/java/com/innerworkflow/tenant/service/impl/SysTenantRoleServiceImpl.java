package com.innerworkflow.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.tenant.dto.TenantRoleSaveDTO;
import com.innerworkflow.tenant.entity.SysTenantRole;
import com.innerworkflow.tenant.entity.SysTenantRoleMenu;
import com.innerworkflow.tenant.entity.SysTenantUserRole;
import com.innerworkflow.tenant.mapper.SysTenantRoleMapper;
import com.innerworkflow.tenant.mapper.SysTenantRoleMenuMapper;
import com.innerworkflow.tenant.mapper.SysTenantUserRoleMapper;
import com.innerworkflow.tenant.service.SysTenantRoleService;
import com.innerworkflow.tenant.vo.TenantRoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysTenantRoleServiceImpl implements SysTenantRoleService {

    private final SysTenantRoleMapper tenantRoleMapper;
    private final SysTenantRoleMenuMapper tenantRoleMenuMapper;
    private final SysTenantUserRoleMapper tenantUserRoleMapper;

    @Override
    public List<TenantRoleVO> listByTenantId(Long tenantId) {
        List<SysTenantRole> roles = tenantRoleMapper.selectList(
                new LambdaQueryWrapper<SysTenantRole>()
                        .eq(SysTenantRole::getTenantId, tenantId)
                        .orderByAsc(SysTenantRole::getRoleSort));
        return roles.stream().map(this::toTenantRoleVO).collect(Collectors.toList());
    }

    @Override
    public TenantRoleVO getById(Long id) {
        SysTenantRole role = tenantRoleMapper.selectById(id);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        return toTenantRoleVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(TenantRoleSaveDTO dto) {
        SysTenantRole role = new SysTenantRole();
        role.setTenantId(dto.getTenantId());
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setRoleSort(dto.getRoleSort());
        role.setDataScope(dto.getDataScope());
        role.setStatus(dto.getStatus());
        role.setRemark(dto.getRemark());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        tenantRoleMapper.insert(role);

        saveRoleMenus(role.getId(), dto.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TenantRoleSaveDTO dto) {
        SysTenantRole role = tenantRoleMapper.selectById(dto.getId());
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setRoleSort(dto.getRoleSort());
        role.setDataScope(dto.getDataScope());
        role.setStatus(dto.getStatus());
        role.setRemark(dto.getRemark());
        role.setUpdateTime(LocalDateTime.now());
        tenantRoleMapper.updateById(role);

        tenantRoleMenuMapper.delete(
                new LambdaQueryWrapper<SysTenantRoleMenu>()
                        .eq(SysTenantRoleMenu::getTenantRoleId, role.getId()));
        saveRoleMenus(role.getId(), dto.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        SysTenantRole role = tenantRoleMapper.selectById(id);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        tenantRoleMapper.deleteById(id);
        tenantRoleMenuMapper.delete(
                new LambdaQueryWrapper<SysTenantRoleMenu>()
                        .eq(SysTenantRoleMenu::getTenantRoleId, id));
        tenantUserRoleMapper.delete(
                new LambdaQueryWrapper<SysTenantUserRole>()
                        .eq(SysTenantUserRole::getTenantRoleId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRole(Long tenantId, Long userId, Long tenantRoleId) {
        Long existing = tenantUserRoleMapper.selectCount(
                new LambdaQueryWrapper<SysTenantUserRole>()
                        .eq(SysTenantUserRole::getTenantId, tenantId)
                        .eq(SysTenantUserRole::getUserId, userId)
                        .eq(SysTenantUserRole::getTenantRoleId, tenantRoleId));
        if (existing > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户已分配该角色");
        }

        SysTenantUserRole userRole = new SysTenantUserRole();
        userRole.setTenantId(tenantId);
        userRole.setUserId(userId);
        userRole.setTenantRoleId(tenantRoleId);
        userRole.setCreateTime(LocalDateTime.now());
        tenantUserRoleMapper.insert(userRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserRole(Long tenantId, Long userId, Long tenantRoleId) {
        tenantUserRoleMapper.delete(
                new LambdaQueryWrapper<SysTenantUserRole>()
                        .eq(SysTenantUserRole::getTenantId, tenantId)
                        .eq(SysTenantUserRole::getUserId, userId)
                        .eq(SysTenantUserRole::getTenantRoleId, tenantRoleId));
    }

    @Override
    public List<String> getRoleCodesByTenantUser(Long tenantId, Long userId) {
        return tenantRoleMenuMapper.selectRoleCodesByTenantUser(tenantId, userId);
    }

    @Override
    public List<String> getPermsByTenantUser(Long tenantId, Long userId) {
        return tenantRoleMenuMapper.selectPermsByTenantUser(tenantId, userId);
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        for (Long menuId : menuIds) {
            SysTenantRoleMenu roleMenu = new SysTenantRoleMenu();
            roleMenu.setTenantRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenu.setCreateTime(LocalDateTime.now());
            tenantRoleMenuMapper.insert(roleMenu);
        }
    }

    private TenantRoleVO toTenantRoleVO(SysTenantRole role) {
        TenantRoleVO vo = new TenantRoleVO();
        vo.setId(role.getId());
        vo.setTenantId(role.getTenantId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleSort(role.getRoleSort());
        vo.setDataScope(role.getDataScope());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setCreateTime(role.getCreateTime());

        List<SysTenantRoleMenu> roleMenus = tenantRoleMenuMapper.selectList(
                new LambdaQueryWrapper<SysTenantRoleMenu>()
                        .eq(SysTenantRoleMenu::getTenantRoleId, role.getId()));
        vo.setMenuIds(roleMenus.stream().map(SysTenantRoleMenu::getMenuId).collect(Collectors.toList()));
        return vo;
    }
}
