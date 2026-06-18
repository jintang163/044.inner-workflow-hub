package com.innerworkflow.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "workflow.frontend")
public class FrontendConfig {

    private String url;

    private String approvalDetailPath;

    public String getApprovalDetailUrl(Long instanceId) {
        return url + approvalDetailPath + "?instanceId=" + instanceId;
    }
}
