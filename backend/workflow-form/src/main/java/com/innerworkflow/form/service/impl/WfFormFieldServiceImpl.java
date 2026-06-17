package com.innerworkflow.form.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.JsonUtils;
import com.innerworkflow.form.dto.FieldPermissionCalcDTO;
import com.innerworkflow.form.entity.WfFormVersion;
import com.innerworkflow.form.service.WfFormFieldService;
import com.innerworkflow.form.service.WfFormVersionService;
import com.innerworkflow.form.vo.FieldPermissionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WfFormFieldServiceImpl implements WfFormFieldService {

    @Autowired
    private WfFormVersionService formVersionService;

    @Override
    public FieldPermissionVO calcFieldPermissions(FieldPermissionCalcDTO calcDTO) {
        WfFormVersion version = getFormVersion(calcDTO.getFormId(), calcDTO.getFormVersion());
        if (version == null) {
            throw new BusinessException("表单版本不存在");
        }

        Map<String, String> defaultPermissions = extractDefaultPermissions(version.getFormSchema());
        Map<String, String> mergedPermissions = mergeFieldPermissions(defaultPermissions,
                calcDTO.getNodeFieldPermissions());

        FieldPermissionVO vo = new FieldPermissionVO();
        vo.setFieldPermissions(mergedPermissions);
        vo.setFormSchema(version.getFormSchema());
        return vo;
    }

    @Override
    public Map<String, String> mergeFieldPermissions(Map<String, String> formDefaultPermissions,
                                                     Map<String, String> nodeFieldPermissions) {
        Map<String, String> result = new HashMap<>();
        if (formDefaultPermissions != null) {
            result.putAll(formDefaultPermissions);
        }
        if (nodeFieldPermissions != null) {
            result.putAll(nodeFieldPermissions);
        }
        return result;
    }

    @Override
    public Map<String, Object> mapFormDataToVariables(Object formData, Object fieldMapping) {
        Map<String, Object> result = new HashMap<>();
        if (formData == null || fieldMapping == null) {
            return result;
        }

        try {
            Map<String, Object> formDataMap = JsonUtils.parseObject(
                    JsonUtils.toJsonString(formData), new TypeReference<>() {});
            Map<String, String> mappingMap = JsonUtils.parseObject(
                    JsonUtils.toJsonString(fieldMapping), new TypeReference<>() {});

            if (formDataMap != null && mappingMap != null) {
                for (Map.Entry<String, String> entry : mappingMap.entrySet()) {
                    String formField = entry.getKey();
                    String variableName = entry.getValue();
                    if (formDataMap.containsKey(formField)) {
                        result.put(variableName, formDataMap.get(formField));
                    }
                }
            }
        } catch (Exception e) {
            log.error("表单数据转流程变量失败", e);
            throw new BusinessException("表单数据转换失败");
        }
        return result;
    }

    @Override
    public Map<String, Object> mapVariablesToFormData(Map<String, Object> variables, Object fieldMapping) {
        Map<String, Object> result = new HashMap<>();
        if (variables == null || fieldMapping == null) {
            return result;
        }

        try {
            Map<String, String> mappingMap = JsonUtils.parseObject(
                    JsonUtils.toJsonString(fieldMapping), new TypeReference<>() {});

            if (mappingMap != null) {
                for (Map.Entry<String, String> entry : mappingMap.entrySet()) {
                    String formField = entry.getKey();
                    String variableName = entry.getValue();
                    if (variables.containsKey(variableName)) {
                        result.put(formField, variables.get(variableName));
                    }
                }
            }
        } catch (Exception e) {
            log.error("流程变量转表单数据失败", e);
            throw new BusinessException("流程变量转换失败");
        }
        return result;
    }

    private WfFormVersion getFormVersion(Long formId, Integer version) {
        return formVersionService.getBaseMapper().selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.innerworkflow.form.entity.WfFormVersion>()
                        .eq(com.innerworkflow.form.entity.WfFormVersion::getFormDefinitionId, formId)
                        .eq(com.innerworkflow.form.entity.WfFormVersion::getVersion, version)
        );
    }

    private Map<String, String> extractDefaultPermissions(String formSchema) {
        Map<String, String> permissions = new HashMap<>();
        if (formSchema == null || formSchema.isEmpty()) {
            return permissions;
        }
        try {
            Map<String, Object> schemaMap = JsonUtils.parseMap(formSchema);
            Object properties = schemaMap != null ? schemaMap.get("properties") : null;
            if (properties instanceof Map) {
                Map<?, ?> propsMap = (Map<?, ?>) properties;
                for (Object key : propsMap.keySet()) {
                    permissions.put(key.toString(), "W");
                }
            }
        } catch (Exception e) {
            log.warn("解析表单Schema提取默认权限失败", e);
        }
        return permissions;
    }
}
