package com.innerworkflow.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.innerworkflow.form.entity.SysDictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    @Select("SELECT * FROM sys_dict_data WHERE dict_code = #{dictCode} AND status = 1 AND is_deleted = 0 ORDER BY dict_sort ASC")
    List<SysDictData> selectByDictCode(@Param("dictCode") String dictCode);

    @Select("SELECT * FROM sys_dict_data WHERE dict_code = #{dictCode} AND parent_value = #{parentValue} AND status = 1 AND is_deleted = 0 ORDER BY dict_sort ASC")
    List<SysDictData> selectByDictCodeAndParentValue(@Param("dictCode") String dictCode, @Param("parentValue") String parentValue);
}
