package com.innerworkflow.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.Empty;
import com.innerworkflow.ai.dto.ApprovalAiFeaturesDTO;
import com.innerworkflow.ai.entity.WfAiRecommendation;
import com.innerworkflow.ai.grpc.ApprovalAiServiceGrpc;
import com.innerworkflow.ai.grpc.ApprovalFeatures;
import com.innerworkflow.ai.grpc.ApprovalRecommendation;
import com.innerworkflow.ai.grpc.Factor;
import com.innerworkflow.ai.grpc.GetStatsResponse;
import com.innerworkflow.ai.grpc.TrainingDataItem;
import com.innerworkflow.ai.mapper.WfAiRecommendationMapper;
import com.innerworkflow.ai.service.AiRecommendationService;
import com.innerworkflow.ai.vo.AiFactorVO;
import com.innerworkflow.ai.vo.AiRecommendationVO;
import com.innerworkflow.ai.vo.AiStatsVO;
import com.innerworkflow.approval.entity.WfApprovalHistory;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.mapper.WfApprovalHistoryMapper;
import com.innerworkflow.approval.mapper.WfApprovalTaskMapper;
import com.innerworkflow.approval.mapper.WfProcessInstanceMapper;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.mapper.SysUserMapper;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;
import com.innerworkflow.bpmn.mapper.WfProcessDefinitionMapper;
import com.innerworkflow.common.enums.TaskActionEnum;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.IdGenerator;
import com.innerworkflow.common.util.JsonUtils;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private final ApprovalAiServiceGrpc.ApprovalAiServiceBlockingStub aiStub;
    private final ApprovalAiServiceGrpc.ApprovalAiServiceStub aiAsyncStub;
    private final WfAiRecommendationMapper recommendationMapper;
    private final WfProcessInstanceMapper instanceMapper;
    private final WfApprovalTaskMapper taskMapper;
    private final WfApprovalHistoryMapper historyMapper;
    private final SysUserMapper userMapper;

    @Override
    public AiRecommendationVO getRecommendation(Long taskId) {
        LambdaQueryWrapper<WfAiRecommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfAiRecommendation::getTaskId, taskId);
        wrapper.orderByDesc(WfAiRecommendation::getCreateTime);
        wrapper.last("LIMIT 1");
        WfAiRecommendation existing = recommendationMapper.selectOne(wrapper);

        if (existing != null) {
            return convertToVO(existing);
        }

        WfApprovalTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("审批任务不存在");
        }

        WfProcessInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            throw new BusinessException("流程实例不存在");
        }

        ApprovalAiFeaturesDTO features = buildFeaturesFromTask(task, instance);
        return computeAndSaveRecommendation(features);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiRecommendationVO computeAndSaveRecommendation(ApprovalAiFeaturesDTO features) {
        ApprovalFeatures grpcFeatures = convertToGrpcFeatures(features);
        ApprovalRecommendation grpcRecommendation = aiStub.predict(grpcFeatures);

        WfAiRecommendation recommendation = new WfAiRecommendation();
        recommendation.setId(IdGenerator.nextId());
        recommendation.setTaskId(features.getInstanceId() != null ? findTaskIdByInstanceId(features.getInstanceId(), features.getApproverId()) : null);
        recommendation.setInstanceId(features.getInstanceId());
        recommendation.setApproverId(features.getApproverId());
        recommendation.setApproveProbability((double) grpcRecommendation.getApproveProbability());
        recommendation.setRecommendedAction(grpcRecommendation.getRecommendedAction());
        recommendation.setReason(grpcRecommendation.getReason());
        recommendation.setFactorsJson(JsonUtils.toJsonString(convertFactorsToList(grpcRecommendation.getFactorsList())));
        recommendation.setModelVersion(grpcRecommendation.getModelVersion());
        recommendation.setInferenceMs(grpcRecommendation.getInferenceMs());
        recommendation.setAdopted(0);
        recommendation.setCreateTime(LocalDateTime.now());

        recommendationMapper.insert(recommendation);

        if (recommendation.getTaskId() != null) {
            WfApprovalTask task = new WfApprovalTask();
            task.setId(recommendation.getTaskId());
            task.setAiRecommendationId(recommendation.getId());
            taskMapper.updateById(task);
        }

        return convertToVO(recommendation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordAdoption(Long recommendationId, Integer adopted) {
        WfAiRecommendation recommendation = recommendationMapper.selectById(recommendationId);
        if (recommendation == null) {
            throw new BusinessException("推荐记录不存在");
        }

        recommendation.setAdopted(adopted);
        recommendation.setActionTime(LocalDateTime.now());
        recommendationMapper.updateById(recommendation);
    }

    @Override
    public AiStatsVO getStats() {
        GetStatsResponse grpcStats = aiStub.getStats(Empty.newBuilder().build());

        AiStatsVO stats = new AiStatsVO();
        stats.setTotalTrainedSamples(grpcStats.getTotalTrainedSamples());
        stats.setLastTrainingTime(grpcStats.getLastTrainingTime());
        stats.setCurrentModelVersion(grpcStats.getCurrentModelVersion());
        stats.setAccuracy((double) grpcStats.getAccuracy());

        Map<String, Double> featureImportance = new HashMap<>();
        grpcStats.getFeatureImportanceMap().forEach((key, value) -> featureImportance.put(key, (double) value));
        stats.setFeatureImportance(featureImportance);

        LambdaQueryWrapper<WfAiRecommendation> allWrapper = new LambdaQueryWrapper<>();
        Long totalRecommendations = recommendationMapper.selectCount(allWrapper);
        stats.setTotalRecommendations(totalRecommendations);

        if (totalRecommendations > 0) {
            LambdaQueryWrapper<WfAiRecommendation> adoptedWrapper = new LambdaQueryWrapper<>();
            adoptedWrapper.eq(WfAiRecommendation::getAdopted, 1);
            Long adoptedCount = recommendationMapper.selectCount(adoptedWrapper);
            stats.setAdoptionRate(adoptedCount * 100.0 / totalRecommendations);
        } else {
            stats.setAdoptionRate(0.0);
        }

        return stats;
    }

    @Override
    public void triggerWeeklyTraining() {
        log.info("开始执行周度AI模型训练...");
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        LambdaQueryWrapper<WfApprovalHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(WfApprovalHistory::getOperateTime, sixMonthsAgo);
        wrapper.in(WfApprovalHistory::getActivityType, TaskActionEnum.AGREE.getCode(), TaskActionEnum.REJECT.getCode());
        List<WfApprovalHistory> histories = historyMapper.selectList(wrapper);

        if (histories.isEmpty()) {
            log.info("没有可用于训练的历史数据");
            return;
        }

        log.info("获取到{}条历史审批数据，开始发送训练...", histories.size());

        CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<TrainingDataItem> requestObserver = aiAsyncStub.withDeadlineAfter(30, TimeUnit.MINUTES)
                .train(new StreamObserver<>() {
                    @Override
                    public void onNext(Empty value) {
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.error("AI模型训练失败: {}", t.getMessage(), t);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        log.info("AI模型训练完成");
                        latch.countDown();
                    }
                });

        try {
            for (WfApprovalHistory history : histories) {
                try {
                    TrainingDataItem item = buildTrainingDataItem(history);
                    if (item != null) {
                        requestObserver.onNext(item);
                    }
                } catch (Exception e) {
                    log.warn("构建训练数据失败, historyId={}, error={}", history.getId(), e.getMessage());
                }
            }
            requestObserver.onCompleted();
            latch.await(30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("发送训练数据失败: {}", e.getMessage(), e);
            requestObserver.onError(e);
        }
    }

    private ApprovalAiFeaturesDTO buildFeaturesFromTask(WfApprovalTask task, WfProcessInstance instance) {
        ApprovalAiFeaturesDTO features = new ApprovalAiFeaturesDTO();
        features.setInstanceId(instance.getId());
        features.setDepartmentId(instance.getStartDeptId());
        features.setInitiatorId(instance.getStartUserId());
        features.setApproverId(task.getAssigneeId());
        features.setProcessKey(instance.getProcessKey());
        features.setBusinessLineId(instance.getBusinessLineId());
        features.setPriority(instance.getPriority());
        features.setFormData(instance.getFormData());

        BigDecimal amount = extractAmountFromFormData(instance.getFormData());
        features.setAmount(amount);

        if (instance.getStartUserId() != null) {
            SysUser initiator = userMapper.selectById(instance.getStartUserId());
            if (initiator != null) {
                features.setInitiatorLevel(initiator.getUserType());
            }
        }

        return features;
    }

    private ApprovalFeatures convertToGrpcFeatures(ApprovalAiFeaturesDTO dto) {
        ApprovalFeatures.Builder builder = ApprovalFeatures.newBuilder()
                .setInstanceId(dto.getInstanceId() != null ? dto.getInstanceId().toString() : "")
                .setAmount(dto.getAmount() != null ? dto.getAmount().doubleValue() : 0.0)
                .setDepartmentId(dto.getDepartmentId() != null ? dto.getDepartmentId() : 0L)
                .setInitiatorId(dto.getInitiatorId() != null ? dto.getInitiatorId() : 0L)
                .setInitiatorLevel(dto.getInitiatorLevel() != null ? dto.getInitiatorLevel() : 0)
                .setApproverId(dto.getApproverId() != null ? dto.getApproverId() : 0L)
                .setProcessKey(dto.getProcessKey() != null ? dto.getProcessKey() : "")
                .setBusinessLineId(dto.getBusinessLineId() != null ? dto.getBusinessLineId() : 0L)
                .setPriority(dto.getPriority() != null ? dto.getPriority() : 0)
                .setFormDataJson(dto.getFormData() != null ? JsonUtils.toJsonString(dto.getFormData()) : "");
        return builder.build();
    }

    private AiRecommendationVO convertToVO(WfAiRecommendation entity) {
        AiRecommendationVO vo = new AiRecommendationVO();
        vo.setId(entity.getId());
        vo.setTaskId(entity.getTaskId());
        vo.setApproveProbability(entity.getApproveProbability());
        vo.setRecommendedAction(entity.getRecommendedAction());
        vo.setRecommendedActionName(getActionName(entity.getRecommendedAction()));
        vo.setReason(entity.getReason());
        vo.setModelVersion(entity.getModelVersion());
        vo.setInferenceMs(entity.getInferenceMs());
        vo.setAdopted(entity.getAdopted());
        vo.setAdoptedName(getAdoptedName(entity.getAdopted()));

        if (entity.getFactorsJson() != null) {
            try {
                vo.setFactors(JsonUtils.parseObject(entity.getFactorsJson(), new TypeReference<List<AiFactorVO>>() {}));
            } catch (Exception e) {
                log.warn("解析factorsJson失败: {}", e.getMessage());
                vo.setFactors(new ArrayList<>());
            }
        }

        return vo;
    }

    private List<AiFactorVO> convertFactorsToList(List<Factor> factorsList) {
        List<AiFactorVO> result = new ArrayList<>();
        for (Factor factor : factorsList) {
            AiFactorVO vo = new AiFactorVO();
            vo.setKey(factor.getKey());
            vo.setValue(factor.getValue());
            vo.setWeight((double) factor.getWeight());
            result.add(vo);
        }
        return result;
    }

    private Long findTaskIdByInstanceId(Long instanceId, Long approverId) {
        if (instanceId == null || approverId == null) {
            return null;
        }
        LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalTask::getInstanceId, instanceId);
        wrapper.eq(WfApprovalTask::getAssigneeId, approverId);
        wrapper.orderByDesc(WfApprovalTask::getCreateTime);
        wrapper.last("LIMIT 1");
        WfApprovalTask task = taskMapper.selectOne(wrapper);
        return task != null ? task.getId() : null;
    }

    private TrainingDataItem buildTrainingDataItem(WfApprovalHistory history) {
        WfProcessInstance instance = instanceMapper.selectById(history.getInstanceId());
        if (instance == null) {
            return null;
        }

        WfApprovalTask task = findTaskByHistory(history);
        if (task == null) {
            return null;
        }

        boolean approved = TaskActionEnum.AGREE.getCode().equals(history.getActivityType());

        TrainingDataItem.Builder builder = TrainingDataItem.newBuilder()
                .setInstanceId(instance.getId().toString())
                .setDepartmentId(instance.getStartDeptId() != null ? instance.getStartDeptId() : 0L)
                .setInitiatorId(instance.getStartUserId() != null ? instance.getStartUserId() : 0L)
                .setApproverId(task.getAssigneeId() != null ? task.getAssigneeId() : 0L)
                .setProcessKey(instance.getProcessKey() != null ? instance.getProcessKey() : "")
                .setBusinessLineId(instance.getBusinessLineId() != null ? instance.getBusinessLineId() : 0L)
                .setPriority(instance.getPriority() != null ? instance.getPriority() : 0)
                .setFormDataJson(instance.getFormData() != null ? JsonUtils.toJsonString(instance.getFormData()) : "")
                .setApproved(approved);

        BigDecimal amount = extractAmountFromFormData(instance.getFormData());
        builder.setAmount(amount != null ? amount.doubleValue() : 0.0);

        if (instance.getStartUserId() != null) {
            SysUser initiator = userMapper.selectById(instance.getStartUserId());
            if (initiator != null && initiator.getUserType() != null) {
                builder.setInitiatorLevel(initiator.getUserType());
            }
        }

        return builder.build();
    }

    private WfApprovalTask findTaskByHistory(WfApprovalHistory history) {
        LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalTask::getInstanceId, history.getInstanceId());
        wrapper.eq(WfApprovalTask::getAssigneeId, history.getOperatorId());
        wrapper.orderByDesc(WfApprovalTask::getCreateTime);
        wrapper.last("LIMIT 1");
        return taskMapper.selectOne(wrapper);
    }

    private BigDecimal extractAmountFromFormData(Object formData) {
        if (formData == null) {
            return BigDecimal.ZERO;
        }
        try {
            Map<String, Object> formMap = JsonUtils.objectToMap(formData);
            if (formMap != null) {
                Object amountObj = formMap.get("amount");
                if (amountObj != null) {
                    if (amountObj instanceof BigDecimal) {
                        return (BigDecimal) amountObj;
                    } else if (amountObj instanceof Number) {
                        return BigDecimal.valueOf(((Number) amountObj).doubleValue());
                    } else if (amountObj instanceof String) {
                        return new BigDecimal((String) amountObj);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从表单数据提取金额失败: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private String getActionName(Integer action) {
        if (action == null) {
            return "";
        }
        return switch (action) {
            case 1 -> "同意";
            case 2 -> "拒绝";
            default -> "未知";
        };
    }

    private String getAdoptedName(Integer adopted) {
        if (adopted == null) {
            return "未处理";
        }
        return switch (adopted) {
            case 0 -> "未采用";
            case 1 -> "已采用";
            case 2 -> "已忽略";
            default -> "未知";
        };
    }
}
