package com.innerworkflow.form.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class FormDefinitionQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String formName;

    private String formKey;

    private Long businessLineId;

    private Integer status;
}
