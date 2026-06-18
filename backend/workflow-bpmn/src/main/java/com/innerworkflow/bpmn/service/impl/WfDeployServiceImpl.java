package com.innerworkflow.bpmn.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innerworkflow.bpmn.dto.WfProcessDeployDTO;
import com.innerworkflow.bpmn.dto.WfProcessDesignSaveDTO;
import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.entity.WfSequenceFlowConfig;
import com.innerworkflow.bpmn.service.*;
import com.innerworkflow.bpmn.vo.WfProcessDesignVO;
import com.innerworkflow.common.enums.ApproveTypeEnum;
import com.innerworkflow.common.enums.AssigneeTypeEnum;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfDeployServiceImpl implements WfDeployService {

    private static final String FLOWABLE_EXT_NAMESPACE = "http://flowable.org/bpmn";
    private static final String EXT_ATTR_ASSIGNEE_TYPE = "assigneeType";
    private static final String EXT_ATTR_NODE_CONFIG = "nodeConfig";
    private static final String TASK_LISTENER_BEAN = "taskAssigneeHandler";

    private final WfProcessDefinitionService processDefinitionService;
    private final WfProcessVersionService processVersionService;
    private final WfNodeConfigService nodeConfigService;
    private final WfSequenceFlowConfigService sequenceFlowConfigService;
    private final RepositoryService repositoryService;
    private final ObjectMapper objectMapper;

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
        designVO.setProcessDefinition(processDefinition);

        WfProcessVersion currentVersion = processVersionService.getCurrentVersion(processDefinitionId);
        if (currentVersion != null) {
            designVO.setCurrentVersion(currentVersion);
            designVO.setBpmnXml(currentVersion.getBpmnXml());
            designVO.setNodeConfigs(nodeConfigService.listByProcessVersionId(currentVersion.getId()));
            designVO.setSequenceFlowConfigs(sequenceFlowConfigService.listByProcessVersionId(currentVersion.getId()));
            designVO.setGlobalNotifyConfig(currentVersion.getGlobalNotifyConfig());
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
            draftVersion.setGlobalNotifyConfig(saveDTO.getGlobalNotifyConfig());
            processVersionService.save(draftVersion);
        } else {
            draftVersion.setBpmnXml(saveDTO.getBpmnXml());
            draftVersion.setGlobalNotifyConfig(saveDTO.getGlobalNotifyConfig());
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

        Long draftVersionId = draftVersion.getId();
        List<WfNodeConfig> nodeConfigs = nodeConfigService.listByProcessVersionId(draftVersionId);
        List<WfSequenceFlowConfig> sequenceFlowConfigs = sequenceFlowConfigService.listByProcessVersionId(draftVersionId);

        if (deployDTO.getNodeConfigs() != null && !deployDTO.getNodeConfigs().isEmpty()) {
            nodeConfigs = deployDTO.getNodeConfigs();
        }
        if (deployDTO.getSequenceFlowConfigs() != null && !deployDTO.getSequenceFlowConfigs().isEmpty()) {
            sequenceFlowConfigs = deployDTO.getSequenceFlowConfigs();
        }

        Map<String, WfNodeConfig> nodeConfigMap = buildNodeConfigMap(nodeConfigs);
        Map<String, WfSequenceFlowConfig> sequenceFlowConfigMap = buildSequenceFlowConfigMap(sequenceFlowConfigs);

        String enhancedBpmnXml = enhanceBpmnXml(draftVersion.getBpmnXml(), nodeConfigMap, sequenceFlowConfigMap);
        log.info("BPMN XML增强完成, processKey={}", processDefinition.getProcessKey());

        int newVersionNo = processDefinition.getCurrentVersion() == null ? 1 : processDefinition.getCurrentVersion() + 1;

        Deployment deployment = repositoryService.createDeployment()
                .name(processDefinition.getProcessName())
                .key(processDefinition.getProcessKey())
                .addString(processDefinition.getProcessKey() + ".bpmn20.xml", enhancedBpmnXml)
                .deploy();

        ProcessDefinition flowableProcessDef = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        if (flowableProcessDef == null) {
            throw new BusinessException(ResultCode.PROCESS_ERROR, "流程部署失败，未找到流程定义");
        }

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
        newVersion.setBpmnXml(enhancedBpmnXml);
        newVersion.setFormId(deployDTO.getFormId() != null ? deployDTO.getFormId() : processDefinition.getFormId());
        newVersion.setFormVersion(deployDTO.getFormVersion() != null ? deployDTO.getFormVersion() : 1);
        newVersion.setVersionRemark(deployDTO.getVersionRemark());
        newVersion.setPublishBy(SecurityUtils.getCurrentUserId());
        newVersion.setPublishTime(LocalDateTime.now());
        newVersion.setIsCurrent(1);
        newVersion.setSuspendStatus(0);
        newVersion.setGlobalNotifyConfig(draftVersion.getGlobalNotifyConfig());
        processVersionService.save(newVersion);

        saveNodeConfigsForNewVersion(nodeConfigs, newVersion.getId(), processDefinition.getProcessKey());
        saveSequenceFlowConfigsForNewVersion(sequenceFlowConfigs, newVersion.getId(), processDefinition.getProcessKey());

        processDefinition.setCurrentVersion(newVersionNo);
        processDefinition.setProcessStatus(1);
        if (deployDTO.getFormId() != null) {
            processDefinition.setFormId(deployDTO.getFormId());
        }
        processDefinitionService.updateById(processDefinition);

        log.info("流程部署成功, processKey={}, version={}, deploymentId={}, flowableProcessDefId={}",
                processDefinition.getProcessKey(), newVersionNo, deployment.getId(), flowableProcessDef.getId());
    }

    private String enhanceBpmnXml(String bpmnXml, Map<String, WfNodeConfig> nodeConfigMap,
                                   Map<String, WfSequenceFlowConfig> sequenceFlowConfigMap) {
        try {
            BpmnModel bpmnModel = parseBpmnXml(bpmnXml);

            Process process = bpmnModel.getProcesses().get(0);
            Collection<FlowElement> flowElements = process.getFlowElements();

            for (FlowElement flowElement : flowElements) {
                if (flowElement instanceof UserTask userTask) {
                    enhanceUserTask(userTask, nodeConfigMap.get(userTask.getId()));
                } else if (flowElement instanceof SequenceFlow sequenceFlow) {
                    enhanceSequenceFlow(sequenceFlow, sequenceFlowConfigMap.get(sequenceFlow.getId()));
                }
            }

            return convertBpmnModelToXml(bpmnModel);
        } catch (Exception e) {
            log.error("增强BPMN XML失败", e);
            throw new BusinessException(ResultCode.PROCESS_ERROR, "增强BPMN XML失败: " + e.getMessage());
        }
    }

    private BpmnModel parseBpmnXml(String bpmnXml) throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8))) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            BpmnXMLConverter converter = new BpmnXMLConverter();
            return converter.convertToBpmnModel(reader);
        }
    }

    private String convertBpmnModelToXml(BpmnModel bpmnModel) throws Exception {
        new BpmnAutoLayout(bpmnModel).execute();
        BpmnXMLConverter converter = new BpmnXMLConverter();
        byte[] xmlBytes = converter.convertToXML(bpmnModel);
        return new String(xmlBytes, StandardCharsets.UTF_8);
    }

    private void enhanceUserTask(UserTask userTask, WfNodeConfig nodeConfig) {
        if (nodeConfig == null) {
            return;
        }

        if (nodeConfig.getNodeName() != null && !nodeConfig.getNodeName().isEmpty()) {
            userTask.setName(nodeConfig.getNodeName());
        }

        AssigneeTypeEnum assigneeType = AssigneeTypeEnum.getByCode(nodeConfig.getAssigneeType());
        if (assigneeType != null) {
            userTask.addAttribute(new ExtensionAttribute(EXT_ATTR_ASSIGNEE_TYPE, FLOWABLE_EXT_NAMESPACE,
                    EXT_ATTR_ASSIGNEE_TYPE, String.valueOf(nodeConfig.getAssigneeType())));

            if (assigneeType == AssigneeTypeEnum.FIXED_USER && nodeConfig.getAssigneeValue() != null) {
                if (nodeConfig.getAssigneeValue().size() == 1) {
                    userTask.setAssignee(String.valueOf(nodeConfig.getAssigneeValue().get(0)));
                } else {
                    for (Long userId : nodeConfig.getAssigneeValue()) {
                        userTask.addCandidateUser(String.valueOf(userId));
                    }
                }
            }
        }

        if (nodeConfig.getTimeoutHours() != null && nodeConfig.getTimeoutHours() > 0) {
            userTask.setDueDate("${nowPlusHours(" + nodeConfig.getTimeoutHours() + ")}");
        }

        addNodeConfigExtension(userTask, nodeConfig);
        addTaskListeners(userTask);
        addMultiInstanceConfig(userTask, nodeConfig);
    }

    private void addNodeConfigExtension(UserTask userTask, WfNodeConfig nodeConfig) {
        try {
            String nodeConfigJson = objectMapper.writeValueAsString(nodeConfig);
            userTask.addAttribute(new ExtensionAttribute(EXT_ATTR_NODE_CONFIG, FLOWABLE_EXT_NAMESPACE,
                    EXT_ATTR_NODE_CONFIG, nodeConfigJson));
        } catch (Exception e) {
            log.warn("序列化节点配置失败, nodeId={}", nodeConfig.getNodeId(), e);
        }
    }

    private void addTaskListeners(UserTask userTask) {
        FlowableListener createListener = new FlowableListener();
        createListener.setEvent("create");
        createListener.setImplementationType("delegateExpression");
        createListener.setImplementation("${" + TASK_LISTENER_BEAN + "}");
        userTask.getTaskListeners().add(createListener);

        FlowableListener assignmentListener = new FlowableListener();
        assignmentListener.setEvent("assignment");
        assignmentListener.setImplementationType("delegateExpression");
        assignmentListener.setImplementation("${" + TASK_LISTENER_BEAN + "}");
        userTask.getTaskListeners().add(assignmentListener);
    }

    private void addMultiInstanceConfig(UserTask userTask, WfNodeConfig nodeConfig) {
        Integer multiInstance = nodeConfig.getMultiInstance();
        Integer approveType = nodeConfig.getApproveType();

        if (multiInstance == null || multiInstance != 1) {
            return;
        }

        MultiInstanceLoopCharacteristics multiInstanceLoop = new MultiInstanceLoopCharacteristics();
        multiInstanceLoop.setInputDataItem("${assigneeList}");
        multiInstanceLoop.setElementVariable("assignee");
        multiInstanceLoop.setElementIndexVariable("loopCounter");

        if (approveType != null) {
            if (approveType.equals(ApproveTypeEnum.ALL_SIGN.getCode())) {
                multiInstanceLoop.setCompletionCondition("${nrOfCompletedInstances == nrOfInstances}");
            } else if (approveType.equals(ApproveTypeEnum.OR_SIGN.getCode())) {
                multiInstanceLoop.setCompletionCondition("${nrOfCompletedInstances > 0}");
            } else if (approveType.equals(ApproveTypeEnum.SEQUENTIAL.getCode())) {
                multiInstanceLoop.setSequential(true);
                multiInstanceLoop.setCompletionCondition("${nrOfCompletedInstances == nrOfInstances}");
            }
        }

        userTask.setAssignee("${assignee}");
        userTask.setLoopCharacteristics(multiInstanceLoop);
    }

    private void enhanceSequenceFlow(SequenceFlow sequenceFlow, WfSequenceFlowConfig flowConfig) {
        if (flowConfig == null) {
            return;
        }

        if (flowConfig.getFlowName() != null && !flowConfig.getFlowName().isEmpty()) {
            sequenceFlow.setName(flowConfig.getFlowName());
        }

        if (flowConfig.getConditionExpression() != null && !flowConfig.getConditionExpression().isEmpty()) {
            sequenceFlow.setConditionExpression(flowConfig.getConditionExpression());
        }
    }

    private Map<String, WfNodeConfig> buildNodeConfigMap(List<WfNodeConfig> nodeConfigs) {
        Map<String, WfNodeConfig> map = new HashMap<>();
        if (nodeConfigs != null) {
            for (WfNodeConfig config : nodeConfigs) {
                if (config.getNodeId() != null) {
                    map.put(config.getNodeId(), config);
                }
            }
        }
        return map;
    }

    private Map<String, WfSequenceFlowConfig> buildSequenceFlowConfigMap(List<WfSequenceFlowConfig> flowConfigs) {
        Map<String, WfSequenceFlowConfig> map = new HashMap<>();
        if (flowConfigs != null) {
            for (WfSequenceFlowConfig config : flowConfigs) {
                if (config.getFlowId() != null) {
                    map.put(config.getFlowId(), config);
                }
            }
        }
        return map;
    }

    private void saveNodeConfigsForNewVersion(List<WfNodeConfig> sourceConfigs, Long targetVersionId, String processKey) {
        if (sourceConfigs == null || sourceConfigs.isEmpty()) {
            return;
        }
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

    private void saveSequenceFlowConfigsForNewVersion(List<WfSequenceFlowConfig> sourceConfigs, Long targetVersionId, String processKey) {
        if (sourceConfigs == null || sourceConfigs.isEmpty()) {
            return;
        }
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
