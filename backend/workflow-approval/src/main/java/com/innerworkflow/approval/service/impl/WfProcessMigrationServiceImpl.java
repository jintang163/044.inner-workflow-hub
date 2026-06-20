package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innerworkflow.approval.dto.WfInstanceMigrationQueryDTO;
import com.innerworkflow.approval.dto.WfProcessMigrationDTO;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.entity.WfProcessMigrationDetail;
import com.innerworkflow.approval.entity.WfProcessMigrationRecord;
import com.innerworkflow.approval.enums.MigrationResultEnum;
import com.innerworkflow.approval.enums.MigrationStatusEnum;
import com.innerworkflow.approval.mapper.WfProcessMigrationDetailMapper;
import com.innerworkflow.approval.mapper.WfProcessMigrationRecordMapper;
import com.innerworkflow.approval.service.WfApprovalTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.service.WfProcessMigrationService;
import com.innerworkflow.approval.vo.*;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.service.WfProcessVersionService;
import com.innerworkflow.common.context.TenantContext;
import com.innerworkflow.common.dto.PageQuery;
import com.innerworkflow.common.enums.TaskStatusEnum;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceMigrationBuilder;
import org.flowable.engine.runtime.ProcessInstanceMigrationValidationResult;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WfProcessMigrationServiceImpl implements WfProcessMigrationService {

    private final WfProcessMigrationRecordMapper recordMapper;
    private final WfProcessMigrationDetailMapper detailMapper;
    private final WfProcessInstanceService instanceService;
    private final WfApprovalTaskService approvalTaskService;
    private final WfProcessVersionService processVersionService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<WfMigrateInstanceVO> pageMigratableInstances(WfInstanceMigrationQueryDTO queryDTO) {
        LambdaQueryWrapper<WfProcessInstance> wrapper = buildQueryWrapper(queryDTO);
        wrapper.eq(WfProcessInstance::getInstanceStatus, 1);

        IPage<WfProcessInstance> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<WfProcessInstance> instancePage = instanceService.page(page, wrapper);

        return convertToMigratePage(instancePage);
    }

    @Override
    public List<WfMigrateInstanceVO> listMigratableInstances(WfInstanceMigrationQueryDTO queryDTO) {
        LambdaQueryWrapper<WfProcessInstance> wrapper = buildQueryWrapper(queryDTO);
        wrapper.eq(WfProcessInstance::getInstanceStatus, 1);
        wrapper.last("LIMIT 500");
        List<WfProcessInstance> list = instanceService.list(wrapper);
        return convertToMigrateList(list);
    }

    private LambdaQueryWrapper<WfProcessInstance> buildQueryWrapper(WfInstanceMigrationQueryDTO q) {
        LambdaQueryWrapper<WfProcessInstance> wrapper = new LambdaQueryWrapper<>();
        if (q.getProcessDefinitionId() != null) {
            wrapper.eq(WfProcessInstance::getProcessDefinitionId, q.getProcessDefinitionId());
        }
        if (q.getProcessKey() != null && !q.getProcessKey().isEmpty()) {
            wrapper.eq(WfProcessInstance::getProcessKey, q.getProcessKey());
        }
        if (q.getTitle() != null && !q.getTitle().isEmpty()) {
            wrapper.like(WfProcessInstance::getTitle, q.getTitle());
        }
        if (q.getInstanceNo() != null && !q.getInstanceNo().isEmpty()) {
            wrapper.like(WfProcessInstance::getInstanceNo, q.getInstanceNo());
        }
        if (q.getStartUserId() != null) {
            wrapper.eq(WfProcessInstance::getStartUserId, q.getStartUserId());
        }
        wrapper.orderByDesc(WfProcessInstance::getCreateTime);
        return wrapper;
    }

    private IPage<WfMigrateInstanceVO> convertToMigratePage(IPage<WfProcessInstance> page) {
        IPage<WfMigrateInstanceVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(convertToMigrateList(page.getRecords()));
        return result;
    }

    private List<WfMigrateInstanceVO> convertToMigrateList(List<WfProcessInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> defIds = instances.stream()
                .map(WfProcessInstance::getProcessDefinitionId)
                .collect(Collectors.toSet());

        Map<Long, List<WfProcessVersion>> versionMap = new HashMap<>();
        for (Long defId : defIds) {
            List<WfProcessVersion> versions = processVersionService.listByProcessDefinitionId(defId);
            versionMap.put(defId, versions);
        }

        return instances.stream().map(inst -> {
            WfMigrateInstanceVO vo = new WfMigrateInstanceVO();
            BeanUtils.copyProperties(inst, vo);

            List<WfProcessVersion> versions = versionMap.getOrDefault(inst.getProcessDefinitionId(), new ArrayList<>());
            WfProcessVersion currentVersion = versions.stream()
                    .filter(v -> v.getId().equals(inst.getProcessVersionId()))
                    .findFirst().orElse(null);
            WfProcessVersion latestVersion = versions.stream()
                    .filter(v -> v.getIsCurrent() != null && v.getIsCurrent() == 1)
                    .findFirst().orElse(null);

            if (currentVersion != null) {
                vo.setCurrentVersion(currentVersion.getVersion());
            }
            if (latestVersion != null) {
                vo.setLatestVersion(latestVersion.getVersion());
                vo.setLatestVersionId(latestVersion.getId());
                vo.setVersionRemark(latestVersion.getVersionRemark());
            }
            if (currentVersion != null && latestVersion != null) {
                vo.setVersionGap(latestVersion.getVersion() - currentVersion.getVersion());
            }

            boolean canMigrate = latestVersion != null
                    && !latestVersion.getId().equals(inst.getProcessVersionId())
                    && (latestVersion.getSuspendStatus() == null || latestVersion.getSuspendStatus() == 0);
            vo.setCanMigrate(canMigrate);

            if (!canMigrate) {
                if (latestVersion == null) {
                    vo.setMigrateTip("没有可用的新版本");
                } else if (latestVersion.getId().equals(inst.getProcessVersionId())) {
                    vo.setMigrateTip("已是最新版本，无需迁移");
                } else if (latestVersion.getSuspendStatus() != null && latestVersion.getSuspendStatus() == 1) {
                    vo.setMigrateTip("目标版本已挂起");
                }
            }

            vo.setAvailableVersions(versions.stream()
                    .filter(v -> !v.getId().equals(inst.getProcessVersionId()))
                    .filter(v -> v.getSuspendStatus() == null || v.getSuspendStatus() == 0)
                    .sorted(Comparator.comparing(WfProcessVersion::getVersion).reversed())
                    .collect(Collectors.toList()));

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public CompatibilityCheckVO checkCompatibility(Long instanceId, Long targetVersionId) {
        CompatibilityCheckVO result = new CompatibilityCheckVO();
        result.setCompatible(true);
        CompatibilityCheckVO.NodeCheckResult nodeCheck = new CompatibilityCheckVO.NodeCheckResult();
        CompatibilityCheckVO.VariableCheckResult variableCheck = new CompatibilityCheckVO.VariableCheckResult();
        result.setNodeCheck(nodeCheck);
        result.setVariableCheck(variableCheck);

        WfProcessInstance instance = instanceService.getById(instanceId);
        if (instance == null) {
            result.setCompatible(false);
            result.getErrors().add("流程实例不存在");
            return result;
        }

        WfProcessVersion targetVersion = processVersionService.getById(targetVersionId);
        if (targetVersion == null) {
            result.setCompatible(false);
            result.getErrors().add("目标版本不存在");
            return result;
        }

        if (targetVersion.getId().equals(instance.getProcessVersionId())) {
            result.getWarnings().add("目标版本与当前版本相同，无需迁移");
        }

        List<String> currentNodeIds = instance.getCurrentNodeIds();
        if (currentNodeIds != null) {
            nodeCheck.getSourceNodes().addAll(currentNodeIds);
        }

        try {
            ProcessDefinition targetPd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(targetVersion.getFlowableProcessDefId())
                    .singleResult();
            if (targetPd == null) {
                result.setCompatible(false);
                result.getErrors().add("目标流程定义在Flowable中不存在");
                return result;
            }

            BpmnModel bpmnModel = repositoryService.getBpmnModel(targetVersion.getFlowableProcessDefId());
            Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();
            for (FlowElement fe : flowElements) {
                nodeCheck.getTargetNodes().add(fe.getId());
            }

            for (String nodeId : nodeCheck.getSourceNodes()) {
                if (nodeCheck.getTargetNodes().contains(nodeId)) {
                    nodeCheck.getMatchedNodes().add(nodeId);
                } else {
                    nodeCheck.getMissingNodes().add(nodeId);
                }
            }

            if (!nodeCheck.getMissingNodes().isEmpty()) {
                result.setCompatible(false);
                result.getErrors().add("当前节点在目标版本中不存在: " + String.join(", ", nodeCheck.getMissingNodes()));
            } else if (!nodeCheck.getMatchedNodes().isEmpty()) {
                result.getInfos().add("当前节点在目标版本中匹配成功: " + String.join(", ", nodeCheck.getMatchedNodes()));
            }

            try {
                Map<String, Object> vars = runtimeService.getVariables(instance.getFlowableProcessInstId());
                if (vars != null) {
                    variableCheck.getSourceVariables().addAll(vars.keySet());
                }
                variableCheck.getMatchedVariables().addAll(variableCheck.getSourceVariables());
                result.getInfos().add("流程变量数量: " + variableCheck.getSourceVariables().size());
            } catch (Exception e) {
                result.getWarnings().add("无法读取流程变量: " + e.getMessage());
            }

            try {
                ProcessInstanceMigrationBuilder builder = runtimeService.createProcessInstanceMigrationBuilder()
                        .migrateToProcessDefinition(targetVersion.getFlowableProcessDefId());
                ProcessInstanceMigrationValidationResult validation = builder
                        .validateMigration(instance.getFlowableProcessInstId());

                if (!validation.isMigrationValid()) {
                    result.setCompatible(false);
                    List<String> flowableErrors = validation.getValidationMessages();
                    if (flowableErrors != null && !flowableErrors.isEmpty()) {
                        result.getErrors().addAll(flowableErrors);
                    } else {
                        result.getErrors().add("Flowable迁移校验未通过");
                    }
                } else {
                    result.getInfos().add("Flowable迁移校验通过");
                }
            } catch (Exception e) {
                result.getWarnings().add("Flowable迁移校验异常: " + e.getMessage());
            }

        } catch (Exception e) {
            log.error("兼容性检查异常, instanceId={}, targetVersionId={}", instanceId, targetVersionId, e);
            result.setCompatible(false);
            result.getErrors().add("兼容性检查异常: " + e.getMessage());
        }

        if (result.getCompatible()) {
            result.getInfos().add("兼容性检查全部通过，可以迁移");
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WfProcessMigrationResultVO batchMigrate(WfProcessMigrationDTO dto) {
        if (!SecurityUtils.isSuperAdmin()) {
            throw BusinessException.forbidden("只有管理员可以执行流程迁移");
        }

        Long targetVersionId = dto.getTargetVersionId();
        WfProcessVersion targetVersion = processVersionService.getById(targetVersionId);
        if (targetVersion == null) {
            throw BusinessException.notFound("目标版本不存在");
        }
        if (targetVersion.getSuspendStatus() != null && targetVersion.getSuspendStatus() == 1) {
            throw BusinessException.paramError("目标版本已挂起，无法迁移");
        }

        String migrationNo = "MIG" + System.currentTimeMillis() + new Random().nextInt(1000);
        Long currentUserId = SecurityUtils.getCurrentUserId();

        WfProcessMigrationRecord record = new WfProcessMigrationRecord();
        record.setMigrationNo(migrationNo);
        record.setProcessKey(targetVersion.getProcessKey());
        record.setProcessDefinitionId(dto.getProcessDefinitionId());
        record.setTargetVersionId(targetVersionId);
        record.setTargetVersion(targetVersion.getVersion());
        record.setTotalCount(dto.getInstanceIds().size());
        record.setMigrationStatus(MigrationStatusEnum.EXECUTING.getCode());
        record.setRemark(dto.getRemark());
        record.setTenantId(TenantContext.getTenantId());
        record.setCreateBy(currentUserId);
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);

        List<WfProcessInstance> instances = instanceService.listByIds(dto.getInstanceIds());
        Map<Long, WfProcessInstance> instanceMap = instances.stream()
                .collect(Collectors.toMap(WfProcessInstance::getId, i -> i));

        WfProcessMigrationResultVO result = new WfProcessMigrationResultVO();
        result.setRecordId(record.getId());
        result.setMigrationNo(migrationNo);
        result.setProcessKey(targetVersion.getProcessKey());
        result.setTargetVersionId(targetVersionId);
        result.setTargetVersion(targetVersion.getVersion());
        result.setMigrationStatus(MigrationStatusEnum.EXECUTING.getCode());
        result.setCreateTime(LocalDateTime.now());

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        for (Long instanceId : dto.getInstanceIds()) {
            WfProcessMigrationDetail detail = new WfProcessMigrationDetail();
            detail.setRecordId(record.getId());
            detail.setMigrationNo(migrationNo);
            detail.setInstanceId(instanceId);
            detail.setTenantId(TenantContext.getTenantId());
            detail.setCreateBy(currentUserId);
            detail.setCreateTime(LocalDateTime.now());

            WfProcessMigrationResultVO.MigrationDetailItem resultItem =
                    new WfProcessMigrationResultVO.MigrationDetailItem();
            resultItem.setInstanceId(instanceId);

            try {
                WfProcessInstance instance = instanceMap.get(instanceId);
                if (instance == null) {
                    throw new Exception("流程实例不存在");
                }
                detail.setInstanceNo(instance.getInstanceNo());
                detail.setTitle(instance.getTitle());
                detail.setStartUserId(instance.getStartUserId());
                detail.setStartUserName(instance.getStartUserName());
                resultItem.setInstanceNo(instance.getInstanceNo());
                resultItem.setTitle(instance.getTitle());
                resultItem.setStartUserName(instance.getStartUserName());

                detail.setSourceVersionId(instance.getProcessVersionId());
                detail.setSourceFlowableProcInstId(instance.getFlowableProcessInstId());
                detail.setSourceFlowableProcDefId(instance.getFlowableProcessDefId());
                detail.setSourceCurrentNodeIds(instance.getCurrentNodeIds());
                detail.setSourceCurrentApproverIds(instance.getCurrentApproverIds());
                record.setSourceVersionId(instance.getProcessVersionId());

                WfProcessVersion sourceVersion = processVersionService.getById(instance.getProcessVersionId());
                if (sourceVersion != null) {
                    record.setSourceVersion(sourceVersion.getVersion());
                    result.setSourceVersion(sourceVersion.getVersion());
                    result.setSourceVersionId(sourceVersion.getId());
                }

                if (targetVersionId.equals(instance.getProcessVersionId())) {
                    detail.setMigrationResult(MigrationResultEnum.SKIPPED.getCode());
                    detail.setSkipReason("已是目标版本，无需迁移");
                    resultItem.setMigrationResult(MigrationResultEnum.SKIPPED.getCode());
                    resultItem.setMigrationResultName(MigrationResultEnum.SKIPPED.getName());
                    resultItem.setSkipReason("已是目标版本，无需迁移");
                    skipCount++;
                    continue;
                }

                if (!dto.getForceMigrate()) {
                    CompatibilityCheckVO check = checkCompatibility(instanceId, targetVersionId);
                    detail.setCompatibilityCheck(toJson(check));
                    resultItem.setCompatibilityCheck(check);
                    if (!check.getCompatible()) {
                        if (dto.getForceMigrate()) {
                            detail.setMigrationResult(MigrationResultEnum.SKIPPED.getCode());
                            detail.setSkipReason("兼容性检查未通过: " + String.join("; ", check.getErrors()));
                            resultItem.setMigrationResult(MigrationResultEnum.SKIPPED.getCode());
                            resultItem.setMigrationResultName(MigrationResultEnum.SKIPPED.getName());
                            resultItem.setSkipReason("兼容性检查未通过");
                            skipCount++;
                            detailMapper.insert(detail);
                            continue;
                        }
                    }
                }

                doBackup(detail, instance);

                doMigrate(instance, targetVersion);

                instance.setProcessVersionId(targetVersionId);
                instance.setFlowableProcessDefId(targetVersion.getFlowableProcessDefId());
                instanceService.updateById(instance);

                detail.setTargetVersionId(targetVersionId);
                detail.setTargetProcessVersionId(targetVersionId);
                detail.setTargetFlowableProcDefId(targetVersion.getFlowableProcessDefId());
                detail.setMigrationResult(MigrationResultEnum.SUCCESS.getCode());
                detail.setMigrateTime(LocalDateTime.now());

                resultItem.setMigrationResult(MigrationResultEnum.SUCCESS.getCode());
                resultItem.setMigrationResultName(MigrationResultEnum.SUCCESS.getName());
                successCount++;

            } catch (Exception e) {
                log.error("迁移实例失败, instanceId={}", instanceId, e);
                detail.setMigrationResult(MigrationResultEnum.FAILED.getCode());
                detail.setErrorMessage(e.getMessage().length() > 800 ? e.getMessage().substring(0, 800) : e.getMessage());
                resultItem.setMigrationResult(MigrationResultEnum.FAILED.getCode());
                resultItem.setMigrationResultName(MigrationResultEnum.FAILED.getName());
                resultItem.setErrorMessage(e.getMessage());
                failCount++;
            }

            detailMapper.insert(detail);
            result.getDetails().add(resultItem);
        }

        record.setSuccessCount(successCount);
        record.setFailCount(failCount);
        record.setSkipCount(skipCount);
        record.setTotalCount(dto.getInstanceIds().size());

        if (failCount == 0 && skipCount == 0) {
            record.setMigrationStatus(MigrationStatusEnum.SUCCESS.getCode());
            result.setMigrationStatus(MigrationStatusEnum.SUCCESS.getCode());
        } else if (successCount > 0) {
            record.setMigrationStatus(MigrationStatusEnum.PARTIAL_SUCCESS.getCode());
            result.setMigrationStatus(MigrationStatusEnum.PARTIAL_SUCCESS.getCode());
        } else {
            record.setMigrationStatus(MigrationStatusEnum.FAILED.getCode());
            result.setMigrationStatus(MigrationStatusEnum.FAILED.getCode());
        }
        result.setMigrationStatusName(MigrationStatusEnum.getNameByCode(record.getMigrationStatus()));
        record.setUpdateBy(currentUserId);
        record.setUpdateTime(LocalDateTime.now());
        recordMapper.updateById(record);

        result.setTotalCount(record.getTotalCount());
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setSkipCount(skipCount);

        log.info("流程迁移完成, migrationNo={}, total={}, success={}, fail={}, skip={}",
                migrationNo, record.getTotalCount(), successCount, failCount, skipCount);

        return result;
    }

    private void doBackup(WfProcessMigrationDetail detail, WfProcessInstance instance) {
        try {
            detail.setBackupInstanceData(toJson(instance));

            LambdaQueryWrapper<WfApprovalTask> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.eq(WfApprovalTask::getInstanceId, instance.getId());
            taskWrapper.in(WfApprovalTask::getTaskStatus, Arrays.asList(
                    TaskStatusEnum.PENDING.getCode(),
                    TaskStatusEnum.WAITING.getCode()));
            List<WfApprovalTask> tasks = approvalTaskService.list(taskWrapper);
            detail.setBackupTasksData(toJson(tasks));

            Map<String, Object> variables = runtimeService.getVariables(instance.getFlowableProcessInstId());
            detail.setBackupVariablesData(toJson(variables));

        } catch (Exception e) {
            log.warn("备份实例数据失败, instanceId={}, error={}", instance.getId(), e.getMessage());
        }
    }

    private void doMigrate(WfProcessInstance instance, WfProcessVersion targetVersion) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getFlowableProcessInstId())
                .singleResult();
        if (pi == null) {
            throw new RuntimeException("Flowable流程实例不存在");
        }

        ProcessInstanceMigrationBuilder builder = runtimeService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(targetVersion.getFlowableProcessDefId());

        ProcessInstanceMigrationValidationResult validation =
                builder.validateMigration(instance.getFlowableProcessInstId());
        if (!validation.isMigrationValid()) {
            throw new RuntimeException("Flowable迁移校验失败: " + String.join("; ", validation.getValidationMessages()));
        }

        builder.migrate(instance.getFlowableProcessInstId());

        ProcessInstance migrated = runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getFlowableProcessInstId())
                .singleResult();
        if (migrated != null) {
            instance.setFlowableProcessInstId(migrated.getId());
        }
    }

    @Override
    public WfProcessMigrationResultVO getMigrationResult(Long recordId) {
        WfProcessMigrationRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw BusinessException.notFound("迁移记录不存在");
        }

        WfProcessMigrationResultVO result = new WfProcessMigrationResultVO();
        BeanUtils.copyProperties(record, result);
        result.setMigrationStatusName(MigrationStatusEnum.getNameByCode(record.getMigrationStatus()));

        LambdaQueryWrapper<WfProcessMigrationDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(WfProcessMigrationDetail::getRecordId, recordId);
        List<WfProcessMigrationDetail> details = detailMapper.selectList(detailWrapper);

        for (WfProcessMigrationDetail d : details) {
            WfProcessMigrationResultVO.MigrationDetailItem item = new WfProcessMigrationResultVO.MigrationDetailItem();
            item.setDetailId(d.getId());
            item.setInstanceId(d.getInstanceId());
            item.setInstanceNo(d.getInstanceNo());
            item.setTitle(d.getTitle());
            item.setStartUserName(d.getStartUserName());
            item.setMigrationResult(d.getMigrationResult());
            item.setMigrationResultName(MigrationResultEnum.getNameByCode(d.getMigrationResult()));
            item.setSkipReason(d.getSkipReason());
            item.setErrorMessage(d.getErrorMessage());
            item.setSourceCurrentNodeIds(d.getSourceCurrentNodeIds());

            if (d.getCompatibilityCheck() != null && !d.getCompatibilityCheck().isEmpty()) {
                try {
                    item.setCompatibilityCheck(
                            objectMapper.readValue(d.getCompatibilityCheck(), CompatibilityCheckVO.class));
                } catch (Exception ignored) {
                }
            }
            result.getDetails().add(item);
        }

        return result;
    }

    @Override
    public IPage<WfProcessMigrationRecordVO> pageMigrationRecords(Long processDefinitionId, PageQuery query) {
        LambdaQueryWrapper<WfProcessMigrationRecord> wrapper = new LambdaQueryWrapper<>();
        if (processDefinitionId != null) {
            wrapper.eq(WfProcessMigrationRecord::getProcessDefinitionId, processDefinitionId);
        }
        wrapper.orderByDesc(WfProcessMigrationRecord::getCreateTime);

        IPage<WfProcessMigrationRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<WfProcessMigrationRecord> recordPage = recordMapper.selectPage(page, wrapper);

        IPage<WfProcessMigrationRecordVO> result = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        result.setRecords(recordPage.getRecords().stream().map(r -> {
            WfProcessMigrationRecordVO vo = new WfProcessMigrationRecordVO();
            BeanUtils.copyProperties(r, vo);
            vo.setMigrationStatusName(MigrationStatusEnum.getNameByCode(r.getMigrationStatus()));
            return vo;
        }).collect(Collectors.toList()));

        return result;
    }

    @Override
    public byte[] downloadBackup(Long recordId) {
        LambdaQueryWrapper<WfProcessMigrationDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessMigrationDetail::getRecordId, recordId);
        List<WfProcessMigrationDetail> details = detailMapper.selectList(wrapper);

        Map<String, Object> backup = new HashMap<>();
        backup.put("exportTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        backup.put("recordId", recordId);

        List<Map<String, Object>> detailList = new ArrayList<>();
        for (WfProcessMigrationDetail d : details) {
            Map<String, Object> item = new HashMap<>();
            item.put("instanceId", d.getInstanceId());
            item.put("instanceNo", d.getInstanceNo());
            item.put("instanceData", parseJson(d.getBackupInstanceData()));
            item.put("tasksData", parseJson(d.getBackupTasksData()));
            item.put("variablesData", parseJson(d.getBackupVariablesData()));
            detailList.add(item);
        }
        backup.put("details", detailList);

        try {
            return toJson(backup).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("导出备份失败", e);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Object>() {});
        } catch (Exception e) {
            return json;
        }
    }
}
