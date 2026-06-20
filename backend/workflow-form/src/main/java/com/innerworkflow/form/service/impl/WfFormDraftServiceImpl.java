package com.innerworkflow.form.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.IdGenerator;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.form.dto.FormDraftQueryDTO;
import com.innerworkflow.form.dto.FormDraftSaveDTO;
import com.innerworkflow.form.entity.WfFormDraft;
import com.innerworkflow.form.mapper.WfFormDraftMapper;
import com.innerworkflow.form.service.WfFormDraftService;
import com.innerworkflow.form.vo.FormDraftVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
            throw new BusinessException(ResultCode.NOT_FOUND, "表单草稿不存在");
        }
        return convertToVO(draft);
    }

    @Override
    public FormDraftVO getByDraftNo(String draftNo) {
        LambdaQueryWrapper<WfFormDraft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfFormDraft::getDraftNo, draftNo);
        WfFormDraft draft = getOne(wrapper);
        if (draft == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "表单草稿不存在");
        }
        return convertToVO(draft);
    }

    @Override
    public FormDraftVO saveDraft(FormDraftSaveDTO saveDTO) {
        WfFormDraft draft;
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (saveDTO.getId() == null) {
            draft = new WfFormDraft();
            BeanUtils.copyProperties(saveDTO, draft);
            draft.setDraftNo(IdGenerator.generateFormNo());
            draft.setCreatorId(currentUserId);
            draft.setDraftStatus(saveDTO.getDraftStatus() != null ? saveDTO.getDraftStatus() : 1);
            draft.setAutoSaveCount(0);
            if (saveDTO.getAttachmentIds() != null) {
                draft.setAttachmentIds(String.join(",",
                        saveDTO.getAttachmentIds().stream().map(String::valueOf).collect(Collectors.toList())));
            }
            if (saveDTO.getCcUserIds() != null) {
                draft.setCcUserIds(String.join(",",
                        saveDTO.getCcUserIds().stream().map(String::valueOf).collect(Collectors.toList())));
            }
            save(draft);
        } else {
            draft = getById(saveDTO.getId());
            if (draft == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "表单草稿不存在");
            }
            draft.setFormData(saveDTO.getFormData());
            draft.setFormId(saveDTO.getFormId());
            draft.setFormVersion(saveDTO.getFormVersion());
            draft.setFormDefinitionId(saveDTO.getFormDefinitionId());
            draft.setTitle(saveDTO.getTitle());
            draft.setProcessVersionId(saveDTO.getProcessVersionId());
            draft.setProcessName(saveDTO.getProcessName());
            draft.setDraftStatus(saveDTO.getDraftStatus() != null ? saveDTO.getDraftStatus() : draft.getDraftStatus());
            if (saveDTO.getAttachmentIds() != null) {
                draft.setAttachmentIds(String.join(",",
                        saveDTO.getAttachmentIds().stream().map(String::valueOf).collect(Collectors.toList())));
            }
            if (saveDTO.getCcUserIds() != null) {
                draft.setCcUserIds(String.join(",",
                        saveDTO.getCcUserIds().stream().map(String::valueOf).collect(Collectors.toList())));
            }
            updateById(draft);
        }
        return convertToVO(draft);
    }

    @Override
    public FormDraftVO autoSave(FormDraftSaveDTO saveDTO) {
        WfFormDraft draft;
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (saveDTO.getId() != null) {
            draft = getById(saveDTO.getId());
        } else if (StrUtil.isNotBlank(saveDTO.getDraftNo())) {
            LambdaQueryWrapper<WfFormDraft> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WfFormDraft::getDraftNo, saveDTO.getDraftNo());
            wrapper.eq(WfFormDraft::getCreatorId, currentUserId);
            draft = getOne(wrapper);
        } else {
            draft = findLatestDraft(saveDTO.getProcessKey(), currentUserId);
        }

        if (draft == null) {
            draft = new WfFormDraft();
            BeanUtils.copyProperties(saveDTO, draft);
            draft.setDraftNo(IdGenerator.generateFormNo());
            draft.setCreatorId(currentUserId);
            draft.setDraftStatus(2);
            draft.setAutoSaveCount(1);
            draft.setLastAutoSaveTime(LocalDateTime.now());
            if (saveDTO.getAttachmentIds() != null) {
                draft.setAttachmentIds(String.join(",",
                        saveDTO.getAttachmentIds().stream().map(String::valueOf).collect(Collectors.toList())));
            }
            if (saveDTO.getCcUserIds() != null) {
                draft.setCcUserIds(String.join(",",
                        saveDTO.getCcUserIds().stream().map(String::valueOf).collect(Collectors.toList())));
            }
            save(draft);
        } else {
            draft.setFormData(saveDTO.getFormData());
            draft.setFormId(saveDTO.getFormId());
            draft.setFormVersion(saveDTO.getFormVersion());
            draft.setFormDefinitionId(saveDTO.getFormDefinitionId());
            draft.setTitle(saveDTO.getTitle());
            draft.setProcessVersionId(saveDTO.getProcessVersionId());
            draft.setProcessName(saveDTO.getProcessName());
            draft.setDraftStatus(2);
            draft.setAutoSaveCount(draft.getAutoSaveCount() == null ? 1 : draft.getAutoSaveCount() + 1);
            draft.setLastAutoSaveTime(LocalDateTime.now());
            if (saveDTO.getAttachmentIds() != null) {
                draft.setAttachmentIds(String.join(",",
                        saveDTO.getAttachmentIds().stream().map(String::valueOf).collect(Collectors.toList())));
            }
            if (saveDTO.getCcUserIds() != null) {
                draft.setCcUserIds(String.join(",",
                        saveDTO.getCcUserIds().stream().map(String::valueOf).collect(Collectors.toList())));
            }
            updateById(draft);
        }
        return convertToVO(draft);
    }

    @Override
    public FormDraftVO getLatestByProcessKey(String processKey) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        WfFormDraft draft = findLatestDraft(processKey, currentUserId);
        return draft != null ? convertToVO(draft) : null;
    }

    @Override
    public List<FormDraftVO> listMyDrafts(String processKey) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<WfFormDraft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfFormDraft::getCreatorId, currentUserId);
        wrapper.eq(StrUtil.isNotBlank(processKey), WfFormDraft::getProcessKey, processKey);
        wrapper.orderByDesc(WfFormDraft::getUpdateTime);
        return list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public Boolean deleteDraft(Long id) {
        WfFormDraft draft = getById(id);
        if (draft == null) {
            return true;
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!draft.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限删除该草稿");
        }
        return removeById(id);
    }

    @Override
    public Integer cleanExpiredDrafts(Integer days) {
        if (days == null || days <= 0) {
            days = 30;
        }
        LocalDateTime expireTime = LocalDateTime.now().minusDays(days);
        LambdaUpdateWrapper<WfFormDraft> wrapper = new LambdaUpdateWrapper<>();
        wrapper.lt(WfFormDraft::getUpdateTime, expireTime);
        int count = baseMapper.delete(wrapper);
        return count;
    }

    private WfFormDraft findLatestDraft(String processKey, Long userId) {
        LambdaQueryWrapper<WfFormDraft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfFormDraft::getProcessKey, processKey);
        wrapper.eq(WfFormDraft::getCreatorId, userId);
        wrapper.orderByDesc(WfFormDraft::getUpdateTime);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    private FormDraftVO convertToVO(WfFormDraft entity) {
        FormDraftVO vo = new FormDraftVO();
        BeanUtils.copyProperties(entity, vo);

        if (StrUtil.isNotBlank(entity.getAttachmentIds())) {
            vo.setAttachmentIds(Arrays.stream(entity.getAttachmentIds().split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
        }

        if (StrUtil.isNotBlank(entity.getCcUserIds())) {
            vo.setCcUserIds(Arrays.stream(entity.getCcUserIds().split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
        }

        if (entity.getDraftStatus() != null) {
            vo.setDraftStatusName(entity.getDraftStatus() == 1 ? "手动保存" : "自动保存");
        }

        return vo;
    }
}
