package com.innerworkflow.form.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dictName;

    private String dictCode;

    private Integer sourceType;

    private String apiUrl;

    private String apiMethod;

    private String apiHeaders;

    private String apiParams;

    private String apiResponsePath;

    private String cascadeField;

    private String cascadeParent;

    private Integer cacheEnabled;

    private Integer cacheTtl;

    private Integer status;

    private String remark;
}
