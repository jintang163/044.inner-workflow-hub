package com.innerworkflow.approval.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "workflow.minio")
public class MinioConfig {

    private String endpoint = "http://127.0.0.1:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucketName = "workflow-attachments";
    private int presignExpiry = 3600;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
