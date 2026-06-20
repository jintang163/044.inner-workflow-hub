package com.innerworkflow.form.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dictCode;

    private String dictLabel;

    private String dictValue;

    private Integer dictSort;

    private String colorTag;

    private String parentValue;

    private String cssClass;

    private String listClass;

    private Integer isDefault;

    private Integer status;

    private String remark;
}
