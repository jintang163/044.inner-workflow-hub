package com.innerworkflow.approval.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfParallelProgressVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String parallelGatewayId;

    private String parallelGatewayName;

    private Integer totalBranches;

    private Integer completedBranches;

    private Integer activeBranches;

    private Integer rejectStrategy;

    private String rejectStrategyName;

    private Boolean isRejected;

    private List<WfParallelBranchVO> branches;

    private String progressText;
}
