package com.innerworkflow.ai.service;

import com.innerworkflow.ai.dto.ApprovalAiFeaturesDTO;
import com.innerworkflow.ai.vo.AiRecommendationVO;
import com.innerworkflow.ai.vo.AiStatsVO;

public interface AiRecommendationService {

    AiRecommendationVO getRecommendation(Long taskId);

    AiRecommendationVO computeAndSaveRecommendation(ApprovalAiFeaturesDTO features);

    void recordAdoption(Long recommendationId, Integer adopted);

    AiStatsVO getStats();

    void triggerWeeklyTraining();
}
