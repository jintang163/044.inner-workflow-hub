package com.innerworkflow.notify.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "wf_message_template", autoResultMap = true)
public class WfMessageTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String templateCode;

    private String templateName;

    private Long businessLineId;

    private String eventType;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> channelTypes;

    private String dingTemplateId;

    private String wecomTemplateId;

    private String emailSubjectTemplate;

    private String emailContentTemplate;

    private String smsTemplateId;

    private Integer status;
}
