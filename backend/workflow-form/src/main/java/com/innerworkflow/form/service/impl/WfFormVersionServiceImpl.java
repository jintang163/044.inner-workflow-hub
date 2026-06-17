package com.innerworkflow.form.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.form.dto.FormPublishDTO;
import com.innerworkflow.form.entity.WfFormDefinition;
import com.innerworkflow.form.entity.WfFormVersion;
import com.innerworkflow.form.mapper.WfFormVersionMapper;
import com.innerworkflow.form.service.WfFormDefinitionService;
import com.innerworkflow.form.service.WfFormVersionService;
import com.innerworkflow.form.vo.FormVersionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WfFormVersionServiceImpl extends ServiceImpl<WfFormVersionMapper, WfFormVersion>
        implements WfFormVersionService {

    @Autowired
    private WfFormDefinitionService formDefinitionService;

    @Override
    public List<FormVersionVO> listByFormDefinitionId(Long formDefinitionId) {
        LambdaQueryWrapper<WfFormVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfFormVersion::getFormDefinitionId, formDefinitionId);
        wrapper.orderByDesc(WfFormVersion::getVersion);
        List<WfFormVersion> list = list(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public FormVersionVO getCurrentVersion(Long formDefinitionId) {
        LambdaQueryWrapper<WfFormVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfFormVersion::getFormDefinitionId, formDefinitionId);
        wrapper.eq(WfFormVersion::getIsCurrent, 1);
        WfFormVersion version = getOne(wrapper);
        return version != null ? convertToVO(version) : null;
    }

    @Override
    public FormVersionVO getByVersion(Long formDefinitionId, Integer version) {
        LambdaQueryWrapper<WfFormVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfFormVersion::getFormDefinitionId, formDefinitionId);
        wrapper.eq(WfFormVersion::getVersion, version);
        WfFormVersion formVersion = getOne(wrapper);
        return formVersion != null ? convertToVO(formVersion) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormVersionVO publish(FormPublishDTO publishDTO) {
        WfFormDefinition formDefinition = formDefinitionService.getById(publishDTO.getFormDefinitionId());
        if (formDefinition == null) {
            throw new BusinessException("表单定义不存在");
        }

        Integer nextVersion = formDefinition.getCurrentVersion() + 1;

        LambdaUpdateWrapper<WfFormVersion> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfFormVersion::getFormDefinitionId, publishDTO.getFormDefinitionId());
        updateWrapper.set(WfFormVersion::getIsCurrent, 0);
        update(updateWrapper);

        WfFormVersion version = new WfFormVersion();
        version.setFormDefinitionId(publishDTO.getFormDefinitionId());
        version.setFormKey(formDefinition.getFormKey());
        version.setVersion(nextVersion);
        version.setFormSchema(publishDTO.getFormSchema());
        version.setFieldMapping(publishDTO.getFieldMapping());
        version.setVersionRemark(publishDTO.getVersionRemark());
        version.setPublishBy(SecurityUtils.getCurrentUserId());
        version.setPublishTime(LocalDateTime.now());
        version.setIsCurrent(1);
        save(version);

        formDefinition.setCurrentVersion(nextVersion);
        formDefinition.setStatus(1);
        formDefinitionService.updateById(formDefinition);

        return convertToVO(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setCurrentVersion(Long formDefinitionId, Long versionId) {
        WfFormVersion targetVersion = getById(versionId);
        if (targetVersion == null) {
            throw new BusinessException("表单版本不存在");
        }
        if (!targetVersion.getFormDefinitionId().equals(formDefinitionId)) {
            throw new BusinessException("表单版本不属于该表单定义");
        }

        LambdaUpdateWrapper<WfFormVersion> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfFormVersion::getFormDefinitionId, formDefinitionId);
        updateWrapper.set(WfFormVersion::getIsCurrent, 0);
        update(updateWrapper);

        targetVersion.setIsCurrent(1);
        updateById(targetVersion);

        WfFormDefinition formDefinition = formDefinitionService.getById(formDefinitionId);
        if (formDefinition != null) {
            formDefinition.setCurrentVersion(targetVersion.getVersion());
            formDefinitionService.updateById(formDefinition);
        }

        return true;
    }

    private FormVersionVO convertToVO(WfFormVersion entity) {
        FormVersionVO vo = new FormVersionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
