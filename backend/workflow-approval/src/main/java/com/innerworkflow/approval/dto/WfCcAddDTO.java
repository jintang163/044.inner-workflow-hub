package com.innerworkflow.approval.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfCcAddDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long instanceId;

    private List<Long> ccUserIds;

    private String nodeId;

    private String nodeName;

    private Integer ccType;

    private String remark;
}
