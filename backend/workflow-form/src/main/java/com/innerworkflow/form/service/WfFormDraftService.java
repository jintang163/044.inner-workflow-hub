package com.innerworkflow.form.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.form.dto.FormDraftQueryDTO;
import com.innerworkflow.form.dto.FormDraftSaveDTO;
import com.innerworkflow.form.entity.WfFormDraft;
import com.innerworkflow.form.vo.FormDraftVO;

import java.util.List;

public interface WfFormDraftService extends IService<WfFormDraft> {

    Page<FormDraftVO> pageList(FormDraftQueryDTO queryDTO);

    FormDraftVO getDetail(Long id);

    FormDraftVO getByDraftNo(String draftNo);

    FormDraftVO saveDraft(FormDraftSaveDTO saveDTO);

    FormDraftVO autoSave(FormDraftSaveDTO saveDTO);

    FormDraftVO getLatestByProcessKey(String processKey);

    List<FormDraftVO> listMyDrafts(String processKey);

    Boolean deleteDraft(Long id);

    Integer cleanExpiredDrafts(Integer days);
}
