package com.innerworkflow.auth.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String username;

    private String realName;

    private String phone;

    private Integer status;

    private Long deptId;
}
