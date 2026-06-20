package com.innerworkflow.approval.vacation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "workflow.vacation.dingtalk")
public class DingTalkVacationProperties {

    private boolean enabled = false;

    private String appKey;

    private String appSecret;

    private String corpId;

    private int syncDays = 30;

    private int apiCallInterval = 100;
}
