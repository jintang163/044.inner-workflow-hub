package com.innerworkflow.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.innerworkflow.auth.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    List<SysMenu> selectMenuListByUserId(Long userId);

    List<SysMenu> selectMenuListByRoleId(Long roleId);

    List<String> selectPermsByUserId(Long userId);
}
