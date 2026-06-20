package com.innerworkflow.approval.config;

import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.enums.ResultCode;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build()
                );
                log.info("MinIO Bucket [{}] created", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("MinIO Bucket check/create failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "存储服务初始化失败");
        }
    }

    public String putObject(String objectName, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.debug("MinIO putObject success: bucket={}, object={}", minioConfig.getBucketName(), objectName);
            return objectName;
        } catch (Exception e) {
            log.error("MinIO putObject failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "文件上传至存储服务失败");
        }
    }

    public InputStream getObject(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO getObject failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "文件下载失败");
        }
    }

    public String getPresignedUrl(String objectName, Method method) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(method)
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .expiry(minioConfig.getPresignExpiry(), TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO getPresignedUrl failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "获取文件访问地址失败");
        }
    }

    public void removeObject(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.debug("MinIO removeObject success: bucket={}, object={}", minioConfig.getBucketName(), objectName);
        } catch (Exception e) {
            log.error("MinIO removeObject failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "文件删除失败");
        }
    }

    public StatObjectResponse statObject(String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO statObject failed: {}", e.getMessage(), e);
            return null;
        }
    }
}
