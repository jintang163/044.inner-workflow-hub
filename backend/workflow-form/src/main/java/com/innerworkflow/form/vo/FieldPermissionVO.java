package com.innerworkflow.form.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class FieldPermissionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Map<String, String> fieldPermissions;

    private String formSchema;
}
