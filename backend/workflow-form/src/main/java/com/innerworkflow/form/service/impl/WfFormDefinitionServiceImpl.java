package com.innerworkflow.form.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.form.dto.FormDefinitionQueryDTO;
import com.innerworkflow.form.dto.FormDefinitionSaveDTO;
import com.innerworkflow.form.entity.WfFormDefinition;
import com.innerworkflow.form.mapper.WfFormDefinitionMapper;
import com.innerworkflow.form.service.WfFormDefinitionService;
import com.innerworkflow.form.vo.FormDefinitionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class WfFormDefinitionServiceImpl extends ServiceImpl<WfFormDefinitionMapper, WfFormDefinition>
        implements WfFormDefinitionService {

    @Override
    public Page<FormDefinitionVO> pageList(FormDefinitionQueryDTO queryDTO) {
        LambdaQueryWrapper<WfFormDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getFormName()),
                WfFormDefinition::getFormName, queryDTO.getFormName());
        wrapper.like(StrUtil.isNotBlank(queryDTO.getFormKey()),
                WfFormDefinition::getFormKey, queryDTO.getFormKey());
        wrapper.eq(queryDTO.getBusinessLineId() != null,
                WfFormDefinition::getBusinessLineId, queryDTO.getBusinessLineId());
        wrapper.eq(queryDTO.getStatus() != null,
                WfFormDefinition::getStatus, queryDTO.getStatus());
        wrapper.orderByDesc(WfFormDefinition::getCreateTime);

        Page<WfFormDefinition> page = page(queryDTO.buildPage("create_time desc"), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    public FormDefinitionVO getDetail(Long id) {
        WfFormDefinition form = getById(id);
        if (form == null) {
            throw new BusinessException("表单定义不存在");
        }
        return convertToVO(form);
    }

    @Override
    public Boolean saveForm(FormDefinitionSaveDTO saveDTO) {
        if (saveDTO.getId() == null) {
            LambdaQueryWrapper<WfFormDefinition> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WfFormDefinition::getFormKey, saveDTO.getFormKey());
            if (count(wrapper) > 0) {
                throw new BusinessException("表单标识已存在");
            }
            WfFormDefinition form = new WfFormDefinition();
            BeanUtils.copyProperties(saveDTO, form);
            form.setCurrentVersion(0);
            form.setStatus(0);
            return save(form);
        } else {
            WfFormDefinition form = getById(saveDTO.getId());
            if (form == null) {
                throw new BusinessException("表单定义不存在");
            }
            BeanUtils.copyProperties(saveDTO, form);
            return updateById(form);
        }
    }

    @Override
    public Boolean updateStatus(Long id, Integer status) {
        WfFormDefinition form = getById(id);
        if (form == null) {
            throw new BusinessException("表单定义不存在");
        }
        form.setStatus(status);
        return updateById(form);
    }

    @Override
    public Boolean deleteForm(Long id) {
        return removeById(id);
    }

    private FormDefinitionVO convertToVO(WfFormDefinition entity) {
        FormDefinitionVO vo = new FormDefinitionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
