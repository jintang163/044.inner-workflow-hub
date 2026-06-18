package com.innerworkflow.common.service;

import com.innerworkflow.common.dto.ApprovalAiFeaturesDTO;
import com.innerworkflow.common.vo.AiRecommendationVO;
import com.innerworkflow.common.vo.AiStatsVO;

public interface AiRecommendationService {

    AiRecommendationVO getRecommendation(Long taskId);

    AiRecommendationVO computeAndSaveRecommendation(ApprovalAiFeaturesDTO features);

    void recordAdoption(Long recommendationId, Integer adopted);

    AiStatsVO getStats();

    void triggerWeeklyTraining();
}
