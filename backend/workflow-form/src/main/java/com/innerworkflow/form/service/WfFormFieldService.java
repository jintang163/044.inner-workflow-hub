package com.innerworkflow.form.service;

import com.innerworkflow.form.dto.FieldPermissionCalcDTO;
import com.innerworkflow.form.vo.FieldPermissionVO;

import java.util.Map;

public interface WfFormFieldService {

    FieldPermissionVO calcFieldPermissions(FieldPermissionCalcDTO calcDTO);

    Map<String, String> mergeFieldPermissions(Map<String, String> formDefaultPermissions,
                                              Map<String, String> nodeFieldPermissions);

    Map<String, Object> mapFormDataToVariables(Object formData, Object fieldMapping);

    Map<String, Object> mapVariablesToFormData(Map<String, Object> variables, Object fieldMapping);
}
