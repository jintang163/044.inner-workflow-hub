package com.innerworkflow.bpmn.service.impl;

import com.innerworkflow.bpmn.dto.WfProcessDeployDTO;
import com.innerworkflow.bpmn.dto.WfProcessDesignSaveDTO;
import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.entity.WfSequenceFlowConfig;
import com.innerworkflow.bpmn.service.*;
import com.innerworkflow.bpmn.vo.WfProcessDesignVO;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfDeployServiceImpl implements WfDeployService {

    private final WfProcessDefinitionService processDefinitionService;
    private final WfProcessVersionService processVersionService;
    private final WfNodeConfigService nodeConfigService;
    private final WfSequenceFlowConfigService sequenceFlowConfigService;
    private final RepositoryService repositoryService;

    @Override
    public WfProcessDesignVO getDesignData(Long processDefinitionId) {
        WfProcessDefinition processDefinition = processDefinitionService.getById(processDefinitionId);
        if (processDefinition == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程定义不存在");
        }

        WfProcessDesignVO designVO = new WfProcessDesignVO();
        designVO.setProcessDefinitionId(processDefinitionId);
        designVO.setProcessKey(processDefinition.getProcessKey());
        designVO.setProcessName(processDefinition.getProcessName());

        WfProcessVersion currentVersion = processVersionService.getCurrentVersion(processDefinitionId);
        if (currentVersion != null) {
            designVO.setBpmnXml(currentVersion.getBpmnXml());
            designVO.setNodeConfigs(nodeConfigService.listByProcessVersionId(currentVersion.getId()));
            designVO.setSequenceFlowConfigs(sequenceFlowConfigService.listByProcessVersionId(currentVersion.getId()));
        }

        return designVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDesign(WfProcessDesignSaveDTO saveDTO) {
        WfProcessDefinition processDefinition = processDefinitionService.getById(saveDTO.getProcessDefinitionId());
        if (processDefinition == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程定义不存在");
        }

        Integer currentVersionNo = processDefinition.getCurrentVersion();
        if (currentVersionNo == null || currentVersionNo == 0) {
            currentVersionNo = 0;
        }

        WfProcessVersion draftVersion = processVersionService.getByVersion(saveDTO.getProcessDefinitionId(), currentVersionNo);
        if (draftVersion == null) {
            draftVersion = new WfProcessVersion();
            draftVersion.setProcessDefinitionId(saveDTO.getProcessDefinitionId());
            draftVersion.setProcessKey(processDefinition.getProcessKey());
            draftVersion.setVersion(currentVersionNo);
            draftVersion.setBpmnXml(saveDTO.getBpmnXml());
            draftVersion.setIsCurrent(0);
            draftVersion.setSuspendStatus(0);
            draftVersion.setFormId(processDefinition.getFormId());
            draftVersion.setFormVersion(1);
            processVersionService.save(draftVersion);
        } else {
            draftVersion.setBpmnXml(saveDTO.getBpmnXml());
            processVersionService.updateById(draftVersion);
        }

        Long versionId = draftVersion.getId();
        nodeConfigService.removeByProcessVersionId(versionId);
        sequenceFlowConfigService.removeByProcessVersionId(versionId);

        if (saveDTO.getNodeConfigs() != null && !saveDTO.getNodeConfigs().isEmpty()) {
            List<WfNodeConfig> nodeConfigs = new ArrayList<>();
            for (WfProcessDesignSaveDTO.WfNodeConfigDTO dto : saveDTO.getNodeConfigs()) {
                WfNodeConfig nodeConfig = new WfNodeConfig();
                nodeConfig.setProcessVersionId(versionId);
                nodeConfig.setProcessKey(processDefinition.getProcessKey());
                nodeConfig.setNodeId(dto.getNodeId());
                nodeConfig.setNodeName(dto.getNodeName());
                nodeConfig.setNodeType(dto.getNodeType());
                nodeConfig.setApproveType(dto.getApproveType());
                nodeConfig.setMultiInstance(dto.getMultiInstance());
                nodeConfig.setAssigneeType(dto.getAssigneeType());
                nodeConfig.setAssigneeValue(dto.getAssigneeValue());
                nodeConfig.setAssigneeScript(dto.getAssigneeScript());
                nodeConfig.setFormPermission(dto.getFormPermission());
                nodeConfig.setTimeoutStrategy(dto.getTimeoutStrategy());
                nodeConfig.setTimeoutHours(dto.getTimeoutHours());
                nodeConfig.setTimeoutEscalateLevels(dto.getTimeoutEscalateLevels());
                nodeConfig.setNotifyConfig(dto.getNotifyConfig());
                nodeConfig.setEmptyAssigneeStrategy(dto.getEmptyAssigneeStrategy());
                nodeConfig.setRefuseStrategy(dto.getRefuseStrategy());
                nodeConfig.setRefuseTargetNodeId(dto.getRefuseTargetNodeId());
                nodeConfig.setCanAddSign(dto.getCanAddSign());
                nodeConfig.setCanTransfer(dto.getCanTransfer());
                nodeConfig.setCanDelegate(dto.getCanDelegate());
                nodeConfig.setNeedSignature(dto.getNeedSignature());
                nodeConfig.setNeedComment(dto.getNeedComment());
                nodeConfig.setSortOrder(dto.getSortOrder());
                nodeConfigs.add(nodeConfig);
            }
            nodeConfigService.saveBatch(nodeConfigs);
        }

        if (saveDTO.getSequenceFlowConfigs() != null && !saveDTO.getSequenceFlowConfigs().isEmpty()) {
            List<WfSequenceFlowConfig> flowConfigs = new ArrayList<>();
            for (WfProcessDesignSaveDTO.WfSequenceFlowConfigDTO dto : saveDTO.getSequenceFlowConfigs()) {
                WfSequenceFlowConfig flowConfig = new WfSequenceFlowConfig();
                flowConfig.setProcessVersionId(versionId);
                flowConfig.setProcessKey(processDefinition.getProcessKey());
                flowConfig.setFlowId(dto.getFlowId());
                flowConfig.setFlowName(dto.getFlowName());
                flowConfig.setSourceNodeId(dto.getSourceNodeId());
                flowConfig.setTargetNodeId(dto.getTargetNodeId());
                flowConfig.setConditionType(dto.getConditionType());
                flowConfig.setConditionExpression(dto.getConditionExpression());
                flowConfig.setConditionScript(dto.getConditionScript());
                flowConfig.setConditionPriority(dto.getConditionPriority());
                flowConfig.setIsDefault(dto.getIsDefault());
                flowConfigs.add(flowConfig);
            }
            sequenceFlowConfigService.saveBatch(flowConfigs);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deployProcess(WfProcessDeployDTO deployDTO) {
        WfProcessDefinition processDefinition = processDefinitionService.getById(deployDTO.getProcessDefinitionId());
        if (processDefinition == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程定义不存在");
        }

        WfProcessVersion draftVersion = processVersionService.getByVersion(deployDTO.getProcessDefinitionId(), 0);
        if (draftVersion == null || draftVersion.getBpmnXml() == null || draftVersion.getBpmnXml().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先保存流程设计");
        }

        int newVersionNo = processDefinition.getCurrentVersion() + 1;

        Deployment deployment = repositoryService.createDeployment()
                .name(processDefinition.getProcessName())
                .key(processDefinition.getProcessKey())
                .addString(processDefinition.getProcessKey() + ".bpmn20.xml", draftVersion.getBpmnXml())
                .deploy();

        ProcessDefinition flowableProcessDef = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        WfProcessVersion currentVersion = processVersionService.getCurrentVersion(deployDTO.getProcessDefinitionId());
        if (currentVersion != null) {
            currentVersion.setIsCurrent(0);
            processVersionService.updateById(currentVersion);
        }

        WfProcessVersion newVersion = new WfProcessVersion();
        newVersion.setProcessDefinitionId(deployDTO.getProcessDefinitionId());
        newVersion.setProcessKey(processDefinition.getProcessKey());
        newVersion.setVersion(newVersionNo);
        newVersion.setFlowableDeploymentId(deployment.getId());
        newVersion.setFlowableProcessDefId(flowableProcessDef.getId());
        newVersion.setBpmnXml(draftVersion.getBpmnXml());
        newVersion.setFormId(deployDTO.getFormId() != null ? deployDTO.getFormId() : processDefinition.getFormId());
        newVersion.setFormVersion(deployDTO.getFormVersion() != null ? deployDTO.getFormVersion() : 1);
        newVersion.setVersionRemark(deployDTO.getVersionRemark());
        newVersion.setPublishBy(SecurityUtils.getCurrentUserId());
        newVersion.setPublishTime(LocalDateTime.now());
        newVersion.setIsCurrent(1);
        newVersion.setSuspendStatus(0);
        processVersionService.save(newVersion);

        copyNodeConfigs(draftVersion.getId(), newVersion.getId(), processDefinition.getProcessKey());
        copySequenceFlowConfigs(draftVersion.getId(), newVersion.getId(), processDefinition.getProcessKey());

        processDefinition.setCurrentVersion(newVersionNo);
        processDefinition.setProcessStatus(1);
        if (deployDTO.getFormId() != null) {
            processDefinition.setFormId(deployDTO.getFormId());
        }
        processDefinitionService.updateById(processDefinition);

        log.info("流程部署成功, processKey={}, version={}, deploymentId={}",
                processDefinition.getProcessKey(), newVersionNo, deployment.getId());
    }

    private void copyNodeConfigs(Long sourceVersionId, Long targetVersionId, String processKey) {
        List<WfNodeConfig> sourceConfigs = nodeConfigService.listByProcessVersionId(sourceVersionId);
        if (sourceConfigs != null && !sourceConfigs.isEmpty()) {
            List<WfNodeConfig> targetConfigs = new ArrayList<>();
            for (WfNodeConfig source : sourceConfigs) {
                WfNodeConfig target = new WfNodeConfig();
                target.setProcessVersionId(targetVersionId);
                target.setProcessKey(processKey);
                target.setNodeId(source.getNodeId());
                target.setNodeName(source.getNodeName());
                target.setNodeType(source.getNodeType());
                target.setApproveType(source.getApproveType());
                target.setMultiInstance(source.getMultiInstance());
                target.setAssigneeType(source.getAssigneeType());
                target.setAssigneeValue(source.getAssigneeValue());
                target.setAssigneeScript(source.getAssigneeScript());
                target.setFormPermission(source.getFormPermission());
                target.setTimeoutStrategy(source.getTimeoutStrategy());
                target.setTimeoutHours(source.getTimeoutHours());
                target.setTimeoutEscalateLevels(source.getTimeoutEscalateLevels());
                target.setNotifyConfig(source.getNotifyConfig());
                target.setEmptyAssigneeStrategy(source.getEmptyAssigneeStrategy());
                target.setRefuseStrategy(source.getRefuseStrategy());
                target.setRefuseTargetNodeId(source.getRefuseTargetNodeId());
                target.setCanAddSign(source.getCanAddSign());
                target.setCanTransfer(source.getCanTransfer());
                target.setCanDelegate(source.getCanDelegate());
                target.setNeedSignature(source.getNeedSignature());
                target.setNeedComment(source.getNeedComment());
                target.setSortOrder(source.getSortOrder());
                targetConfigs.add(target);
            }
            nodeConfigService.saveBatch(targetConfigs);
        }
    }

    private void copySequenceFlowConfigs(Long sourceVersionId, Long targetVersionId, String processKey) {
        List<WfSequenceFlowConfig> sourceConfigs = sequenceFlowConfigService.listByProcessVersionId(sourceVersionId);
        if (sourceConfigs != null && !sourceConfigs.isEmpty()) {
            List<WfSequenceFlowConfig> targetConfigs = new ArrayList<>();
            for (WfSequenceFlowConfig source : sourceConfigs) {
                WfSequenceFlowConfig target = new WfSequenceFlowConfig();
                target.setProcessVersionId(targetVersionId);
                target.setProcessKey(processKey);
                target.setFlowId(source.getFlowId());
                target.setFlowName(source.getFlowName());
                target.setSourceNodeId(source.getSourceNodeId());
                target.setTargetNodeId(source.getTargetNodeId());
                target.setConditionType(source.getConditionType());
                target.setConditionExpression(source.getConditionExpression());
                target.setConditionScript(source.getConditionScript());
                target.setConditionPriority(source.getConditionPriority());
                target.setIsDefault(source.getIsDefault());
                targetConfigs.add(target);
            }
            sequenceFlowConfigService.saveBatch(targetConfigs);
        }
    }

    @Override
    public void suspendProcess(Long processVersionId) {
        WfProcessVersion processVersion = processVersionService.getById(processVersionId);
        if (processVersion == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程版本不存在");
        }

        if (processVersion.getFlowableProcessDefId() != null) {
            repositoryService.suspendProcessDefinitionById(processVersion.getFlowableProcessDefId());
        }

        processVersion.setSuspendStatus(1);
        processVersionService.updateById(processVersion);
    }

    @Override
    public void activateProcess(Long processVersionId) {
        WfProcessVersion processVersion = processVersionService.getById(processVersionId);
        if (processVersion == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程版本不存在");
        }

        if (processVersion.getFlowableProcessDefId() != null) {
            repositoryService.activateProcessDefinitionById(processVersion.getFlowableProcessDefId());
        }

        processVersion.setSuspendStatus(0);
        processVersionService.updateById(processVersion);
    }

    @Override
    public String generateBpmnXml(Long processDefinitionId) {
        WfProcessDesignVO designVO = getDesignData(processDefinitionId);
        return designVO.getBpmnXml();
    }
}
