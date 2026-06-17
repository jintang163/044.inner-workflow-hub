package com.innerworkflow.notify.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.notify.dto.MessageTemplateQueryDTO;
import com.innerworkflow.notify.dto.MessageTemplateSaveDTO;
import com.innerworkflow.notify.entity.WfMessageTemplate;
import com.innerworkflow.notify.vo.MessageTemplateVO;

import java.util.List;

public interface WfMessageTemplateService extends IService<WfMessageTemplate> {

    Page<MessageTemplateVO> pageList(MessageTemplateQueryDTO queryDTO);

    MessageTemplateVO getDetail(Long id);

    WfMessageTemplate getByCode(String templateCode);

    List<WfMessageTemplate> getByEventType(String eventType, Long businessLineId);

    Boolean saveTemplate(MessageTemplateSaveDTO saveDTO);

    Boolean updateStatus(Long id, Integer status);

    Boolean deleteTemplate(Long id);
}
