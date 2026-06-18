package com.innerworkflow.common.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class AiStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long totalTrainedSamples;

    private String lastTrainingTime;

    private String currentModelVersion;

    private Double accuracy;

    private Map<String, Double> featureImportance;

    private Double adoptionRate;

    private Long totalRecommendations;
}
