package com.innerworkflow.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.auth.dto.MenuSaveDTO;
import com.innerworkflow.auth.entity.SysMenu;
import com.innerworkflow.auth.vo.MenuVO;
import com.innerworkflow.auth.vo.RouterVO;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    List<MenuVO> getMenuTree();

    List<MenuVO> getMenuList(String menuName, Integer status);

    MenuVO getMenuById(Long id);

    void saveMenu(MenuSaveDTO saveDTO);

    void updateMenu(MenuSaveDTO saveDTO);

    void deleteMenu(Long id);

    List<RouterVO> buildRouters(Long userId);

    List<SysMenu> getMenusByUserId(Long userId);

    List<String> getPermsByUserId(Long userId);
}
