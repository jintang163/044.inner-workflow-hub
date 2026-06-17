package com.innerworkflow.form.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.form.dto.FormDefinitionQueryDTO;
import com.innerworkflow.form.dto.FormDefinitionSaveDTO;
import com.innerworkflow.form.entity.WfFormDefinition;
import com.innerworkflow.form.vo.FormDefinitionVO;

public interface WfFormDefinitionService extends IService<WfFormDefinition> {

    Page<FormDefinitionVO> pageList(FormDefinitionQueryDTO queryDTO);

    FormDefinitionVO getDetail(Long id);

    Boolean saveForm(FormDefinitionSaveDTO saveDTO);

    Boolean updateStatus(Long id, Integer status);

    Boolean deleteForm(Long id);
}
