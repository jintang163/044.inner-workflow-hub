package com.innerworkflow.form.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.form.dto.FormPublishDTO;
import com.innerworkflow.form.entity.WfFormVersion;
import com.innerworkflow.form.vo.FormVersionVO;

import java.util.List;

public interface WfFormVersionService extends IService<WfFormVersion> {

    List<FormVersionVO> listByFormDefinitionId(Long formDefinitionId);

    FormVersionVO getCurrentVersion(Long formDefinitionId);

    FormVersionVO getByVersion(Long formDefinitionId, Integer version);

    FormVersionVO publish(FormPublishDTO publishDTO);

    Boolean setCurrentVersion(Long formDefinitionId, Long versionId);
}
