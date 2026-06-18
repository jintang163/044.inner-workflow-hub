package com.innerworkflow.common.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class AiRecommendationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long taskId;

    private Double approveProbability;

    private Integer recommendedAction;

    private String recommendedActionName;

    private String reason;

    private List<AiFactorVO> factors;

    private String modelVersion;

    private Long inferenceMs;

    private Integer adopted;

    private String adoptedName;
}
