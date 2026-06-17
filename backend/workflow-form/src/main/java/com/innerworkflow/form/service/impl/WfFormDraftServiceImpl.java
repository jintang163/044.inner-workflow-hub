package com.innerworkflow.form.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.IdGenerator;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.form.dto.FormDraftQueryDTO;
import com.innerworkflow.form.dto.FormDraftSaveDTO;
import com.innerworkflow.form.entity.WfFormDraft;
import com.innerworkflow.form.mapper.WfFormDraftMapper;
import com.innerworkflow.form.service.WfFormDraftService;
import com.innerworkflow.form.vo.FormDraftVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class WfFormDraftServiceImpl extends ServiceImpl<WfFormDraftMapper, WfFormDraft>
        implements WfFormDraftService {

    @Override
    public Page<FormDraftVO> pageList(FormDraftQueryDTO queryDTO) {
        LambdaQueryWrapper<WfFormDraft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryDTO.getProcessDefinitionId() != null,
                WfFormDraft::getProcessDefinitionId, queryDTO.getProcessDefinitionId());
        wrapper.eq(StrUtil.isNotBlank(queryDTO.getProcessKey()),
                WfFormDraft::getProcessKey, queryDTO.getProcessKey());
        wrapper.eq(queryDTO.getCreatorId() != null,
                WfFormDraft::getCreatorId, queryDTO.getCreatorId());
        wrapper.orderByDesc(WfFormDraft::getUpdateTime);

        Page<WfFormDraft> page = page(queryDTO.buildPage("update_time desc"), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    public FormDraftVO getDetail(Long id) {
        WfFormDraft draft = getById(id);
        if (draft == null) {
            throw new BusinessException("表单草稿不存在");
        }
        return convertToVO(draft);
    }

    @Override
    public FormDraftVO getByDraftNo(String draftNo) {
        LambdaQueryWrapper<WfFormDraft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfFormDraft::getDraftNo, draftNo);
        WfFormDraft draft = getOne(wrapper);
        if (draft == null) {
            throw new BusinessException("表单草稿不存在");
        }
        return convertToVO(draft);
    }

    @Override
    public FormDraftVO saveDraft(FormDraftSaveDTO saveDTO) {
        WfFormDraft draft;
        if (saveDTO.getId() == null) {
            draft = new WfFormDraft();
            BeanUtils.copyProperties(saveDTO, draft);
            draft.setDraftNo(IdGenerator.generateFormNo());
            draft.setCreatorId(SecurityUtils.getCurrentUserId());
            save(draft);
        } else {
            draft = getById(saveDTO.getId());
            if (draft == null) {
                throw new BusinessException("表单草稿不存在");
            }
            draft.setFormData(saveDTO.getFormData());
            draft.setFormId(saveDTO.getFormId());
            draft.setFormVersion(saveDTO.getFormVersion());
            updateById(draft);
        }
        return convertToVO(draft);
    }

    @Override
    public Boolean deleteDraft(Long id) {
        return removeById(id);
    }

    private FormDraftVO convertToVO(WfFormDraft entity) {
        FormDraftVO vo = new FormDraftVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
