package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class WfAgentConfigSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String userName;

    @NotNull(message = "代理人不能为空")
    private Long agentUserId;

    private String agentUserName;

    @NotNull(message = "配置类型不能为空")
    private Integer configType;

    private String processKeys;

    private Integer priority;

    private Integer enabled;

    private String remark;
}
