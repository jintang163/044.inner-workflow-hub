package com.innerworkflow.notify.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageTemplateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String templateCode;

    private String templateName;

    private Long businessLineId;

    private String eventType;

    private List<String> channelTypes;

    private String dingTemplateId;

    private String wecomTemplateId;

    private String emailSubjectTemplate;

    private String emailContentTemplate;

    private String smsTemplateId;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
