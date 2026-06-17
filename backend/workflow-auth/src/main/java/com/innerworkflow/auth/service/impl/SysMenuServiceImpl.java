package com.innerworkflow.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.auth.dto.MenuSaveDTO;
import com.innerworkflow.auth.entity.SysMenu;
import com.innerworkflow.auth.entity.SysRole;
import com.innerworkflow.auth.entity.SysRoleMenu;
import com.innerworkflow.auth.mapper.SysMenuMapper;
import com.innerworkflow.auth.mapper.SysRoleMenuMapper;
import com.innerworkflow.auth.service.SysMenuService;
import com.innerworkflow.auth.service.SysRoleService;
import com.innerworkflow.auth.vo.MenuVO;
import com.innerworkflow.auth.vo.RouterVO;
import com.innerworkflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysRoleService sysRoleService;

    @Override
    public List<MenuVO> getMenuTree() {
        List<SysMenu> allMenus = this.list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder));
        List<MenuVO> menuVOList = allMenus.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return buildMenuTree(menuVOList, 0L);
    }

    @Override
    public List<MenuVO> getMenuList(String menuName, Integer status) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(menuName), SysMenu::getMenuName, menuName)
                .eq(status != null, SysMenu::getStatus, status)
                .orderByAsc(SysMenu::getSortOrder);
        List<SysMenu> list = this.list(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public MenuVO getMenuById(Long id) {
        SysMenu menu = this.getById(id);
        if (menu == null) {
            throw BusinessException.notFound("菜单不存在");
        }
        return convertToVO(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMenu(MenuSaveDTO saveDTO) {
        SysMenu menu = BeanUtil.copyProperties(saveDTO, SysMenu.class);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        this.save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(MenuSaveDTO saveDTO) {
        SysMenu menu = this.getById(saveDTO.getId());
        if (menu == null) {
            throw BusinessException.notFound("菜单不存在");
        }
        BeanUtil.copyProperties(saveDTO, menu);
        this.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        long childCount = this.count(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (childCount > 0) {
            throw BusinessException.paramError("存在子菜单，不允许删除");
        }

        this.removeById(id);
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getMenuId, id));
    }

    @Override
    public List<RouterVO> buildRouters(Long userId) {
        List<SysMenu> menus = getMenusByUserId(userId);
        List<SysMenu> menuTree = buildMenuTree(menus, 0L);
        return buildRouterTree(menuTree);
    }

    @Override
    public List<SysMenu> getMenusByUserId(Long userId) {
        List<SysRole> roles = sysRoleService.getRolesByUserId(userId);
        if (roles.isEmpty()) {
            return List.of();
        }

        boolean isAdmin = roles.stream().anyMatch(r -> "admin".equals(r.getRoleCode()));
        if (isAdmin) {
            return this.list(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, 1)
                    .in(SysMenu::getMenuType, "M", "C")
                    .orderByAsc(SysMenu::getSortOrder));
        }

        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus.isEmpty()) {
            return List.of();
        }

        List<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());

        return this.list(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, 1)
                .in(SysMenu::getMenuType, "M", "C")
                .orderByAsc(SysMenu::getSortOrder));
    }

    @Override
    public List<String> getPermsByUserId(Long userId) {
        List<SysRole> roles = sysRoleService.getRolesByUserId(userId);
        if (roles.isEmpty()) {
            return List.of();
        }

        boolean isAdmin = roles.stream().anyMatch(r -> "admin".equals(r.getRoleCode()));
        if (isAdmin) {
            List<SysMenu> menus = this.list(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, 1)
                    .isNotNull(SysMenu::getPermission));
            return menus.stream()
                    .map(SysMenu::getPermission)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
        }

        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus.isEmpty()) {
            return List.of();
        }

        List<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());

        List<SysMenu> menus = this.list(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, 1)
                .isNotNull(SysMenu::getPermission));

        return menus.stream()
                .map(SysMenu::getPermission)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<MenuVO> buildMenuTree(List<MenuVO> menus, Long parentId) {
        List<MenuVO> result = new ArrayList<>();
        for (MenuVO menu : menus) {
            if (menu.getParentId().equals(parentId)) {
                menu.setChildren(buildMenuTree(menus, menu.getId()));
                result.add(menu);
            }
        }
        return result;
    }

    private List<SysMenu> buildMenuTree(List<SysMenu> menus, Long parentId) {
        List<SysMenu> result = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (menu.getParentId().equals(parentId)) {
                menu.setChildren(buildMenuTree(menus, menu.getId()));
                result.add(menu);
            }
        }
        return result;
    }

    private List<RouterVO> buildRouterTree(List<SysMenu> menus) {
        List<RouterVO> routers = new ArrayList<>();
        for (SysMenu menu : menus) {
            RouterVO router = new RouterVO();
            router.setId(menu.getId());
            router.setName(menu.getPath());
            router.setPath(menu.getPath());
            router.setComponent(menu.getComponent());

            RouterVO.MetaVO meta = new RouterVO.MetaVO();
            meta.setTitle(menu.getMenuName());
            meta.setIcon(menu.getIcon());
            meta.setSortOrder(menu.getSortOrder());
            meta.setVisible(menu.getVisible() == 1);
            router.setMeta(meta);

            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                router.setChildren(buildRouterTree(menu.getChildren()));
            }

            routers.add(router);
        }
        return routers;
    }

    private MenuVO convertToVO(SysMenu menu) {
        return BeanUtil.copyProperties(menu, MenuVO.class);
    }
}
