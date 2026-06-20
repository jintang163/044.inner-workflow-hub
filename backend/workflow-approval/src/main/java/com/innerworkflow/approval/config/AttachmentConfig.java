package com.innerworkflow.approval.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "workflow.attachment")
public class AttachmentConfig {

    private long maxFileSize = 104857600L;

    private String allowedTypes = "jpg,jpeg,png,gif,bmp,webp,pdf,doc,docx,xls,xlsx,ppt,pptx";

    private int maxFileCount = 20;
}
