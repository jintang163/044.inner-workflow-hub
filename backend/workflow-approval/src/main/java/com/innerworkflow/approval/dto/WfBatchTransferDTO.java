package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfBatchTransferDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<String> taskIds;

    private Boolean transferAll = false;

    @NotNull(message = "转审人不能为空")
    private Long targetUserId;

    private String targetUserName;

    private String actionRemark;
}
