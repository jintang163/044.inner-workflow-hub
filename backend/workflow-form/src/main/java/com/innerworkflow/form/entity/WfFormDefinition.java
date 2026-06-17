package com.innerworkflow.form.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_form_definition")
public class WfFormDefinition extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String formKey;

    private String formName;

    private Long businessLineId;

    private String description;

    private Integer currentVersion;

    private Integer status;
}
