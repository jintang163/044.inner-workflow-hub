package com.innerworkflow.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.innerworkflow.approval.entity.WfCommentTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WfCommentTemplateMapper extends BaseMapper<WfCommentTemplate> {

    @Update("UPDATE wf_comment_template SET use_count = use_count + 1 WHERE id = #{id} AND is_deleted = 0")
    int incrementUseCount(@Param("id") Long id);
}
