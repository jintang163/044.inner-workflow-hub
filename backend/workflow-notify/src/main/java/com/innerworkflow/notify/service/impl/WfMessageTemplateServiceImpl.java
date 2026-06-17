package com.innerworkflow.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.notify.dto.MessageTemplateQueryDTO;
import com.innerworkflow.notify.dto.MessageTemplateSaveDTO;
import com.innerworkflow.notify.entity.WfMessageTemplate;
import com.innerworkflow.notify.mapper.WfMessageTemplateMapper;
import com.innerworkflow.notify.service.WfMessageTemplateService;
import com.innerworkflow.notify.vo.MessageTemplateVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfMessageTemplateServiceImpl extends ServiceImpl<WfMessageTemplateMapper, WfMessageTemplate>
        implements WfMessageTemplateService {

    @Override
    public Page<MessageTemplateVO> pageList(MessageTemplateQueryDTO queryDTO) {
        LambdaQueryWrapper<WfMessageTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getTemplateName()),
                WfMessageTemplate::getTemplateName, queryDTO.getTemplateName());
        wrapper.like(StrUtil.isNotBlank(queryDTO.getTemplateCode()),
                WfMessageTemplate::getTemplateCode, queryDTO.getTemplateCode());
        wrapper.eq(StrUtil.isNotBlank(queryDTO.getEventType()),
                WfMessageTemplate::getEventType, queryDTO.getEventType());
        wrapper.eq(queryDTO.getBusinessLineId() != null,
                WfMessageTemplate::getBusinessLineId, queryDTO.getBusinessLineId());
        wrapper.eq(queryDTO.getStatus() != null,
                WfMessageTemplate::getStatus, queryDTO.getStatus());
        wrapper.orderByDesc(WfMessageTemplate::getCreateTime);

        Page<WfMessageTemplate> page = page(queryDTO.buildPage("create_time desc"), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    public MessageTemplateVO getDetail(Long id) {
        WfMessageTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("消息模板不存在");
        }
        return convertToVO(template);
    }

    @Override
    public WfMessageTemplate getByCode(String templateCode) {
        LambdaQueryWrapper<WfMessageTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfMessageTemplate::getTemplateCode, templateCode);
        wrapper.eq(WfMessageTemplate::getStatus, 1);
        return getOne(wrapper);
    }

    @Override
    public List<WfMessageTemplate> getByEventType(String eventType, Long businessLineId) {
        LambdaQueryWrapper<WfMessageTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfMessageTemplate::getEventType, eventType);
        wrapper.eq(WfMessageTemplate::getStatus, 1);
        wrapper.and(w -> w.isNull(WfMessageTemplate::getBusinessLineId)
                .or().eq(businessLineId != null, WfMessageTemplate::getBusinessLineId, businessLineId));
        wrapper.orderByDesc(WfMessageTemplate::getCreateTime);
        return list(wrapper);
    }

    @Override
    public Boolean saveTemplate(MessageTemplateSaveDTO saveDTO) {
        if (saveDTO.getId() == null) {
            LambdaQueryWrapper<WfMessageTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WfMessageTemplate::getTemplateCode, saveDTO.getTemplateCode());
            if (count(wrapper) > 0) {
                throw new BusinessException("模板编码已存在");
            }
            WfMessageTemplate template = new WfMessageTemplate();
            BeanUtils.copyProperties(saveDTO, template);
            if (template.getStatus() == null) {
                template.setStatus(1);
            }
            return save(template);
        } else {
            WfMessageTemplate template = getById(saveDTO.getId());
            if (template == null) {
                throw new BusinessException("消息模板不存在");
            }
            BeanUtils.copyProperties(saveDTO, template);
            return updateById(template);
        }
    }

    @Override
    public Boolean updateStatus(Long id, Integer status) {
        WfMessageTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("消息模板不存在");
        }
        template.setStatus(status);
        return updateById(template);
    }

    @Override
    public Boolean deleteTemplate(Long id) {
        return removeById(id);
    }

    private MessageTemplateVO convertToVO(WfMessageTemplate entity) {
        MessageTemplateVO vo = new MessageTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
