package com.innerworkflow.ai.controller;

import com.innerworkflow.ai.service.AiRecommendationService;
import com.innerworkflow.ai.vo.AiRecommendationVO;
import com.innerworkflow.ai.vo.AiStatsVO;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI智能审批", description = "AI智能审批推荐相关接口")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class WfAiController {

    private final AiRecommendationService aiRecommendationService;

    @Operation(summary = "获取审批推荐")
    @GetMapping("/recommendation/{taskId}")
    public R<AiRecommendationVO> getRecommendation(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        AiRecommendationVO recommendation = aiRecommendationService.getRecommendation(taskId);
        return R.success(recommendation);
    }

    @Operation(summary = "记录推荐采用情况")
    @PostMapping("/adoption/{recommendationId}")
    public R<Void> recordAdoption(
            @Parameter(description = "推荐记录ID") @PathVariable Long recommendationId,
            @Parameter(description = "采用状态：0-未采用 1-已采用 2-已忽略") @RequestParam Integer adopted) {
        aiRecommendationService.recordAdoption(recommendationId, adopted);
        return R.success();
    }

    @Operation(summary = "获取AI服务统计")
    @GetMapping("/stats")
    public R<AiStatsVO> getStats() {
        AiStatsVO stats = aiRecommendationService.getStats();
        return R.success(stats);
    }

    @Operation(summary = "触发周度模型训练")
    @PostMapping("/trigger-training")
    public R<Void> triggerWeeklyTraining() {
        aiRecommendationService.triggerWeeklyTraining();
        return R.success();
    }
}
