package com.innerworkflow.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class LoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String token;

    private UserInfoVO userInfo;
}
