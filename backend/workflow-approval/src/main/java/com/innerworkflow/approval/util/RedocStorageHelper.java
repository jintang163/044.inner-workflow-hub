package com.innerworkflow.approval.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.innerworkflow.approval.config.MinioConfig;
import com.innerworkflow.approval.config.MinioStorageService;
import com.innerworkflow.approval.entity.WfAttachment;
import com.innerworkflow.approval.mapper.WfAttachmentMapper;
import com.innerworkflow.common.util.SecurityUtils;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 直接从字节数组保存附件的辅助工具，避免走MultipartFile上传
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedocStorageHelper {

    private final MinioStorageService minioStorageService;
    private final MinioConfig minioConfig;
    private final WfAttachmentMapper wfAttachmentMapper;

    public WfAttachment saveFromBytes(byte[] bytes, String fileName, String contentType,
                                       String bizType, String bizId, String nodeId) {
        String suffix = cn.hutool.core.io.FileUtil.getSuffix(fileName);
        if (StrUtil.isBlank(suffix)) {
            suffix = "bin";
        }
        String uuidName = IdUtil.fastSimpleUUID() + "." + suffix;
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectName = datePath + "/" + uuidName;
        String bucket = minioConfig.getBucketName();

        try {
            minioStorageService.putObject(objectName,
                    new ByteArrayInputStream(bytes), bytes.length, contentType);
        } catch (Exception e) {
            log.error("红头文件保存到MinIO失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件存储服务不可用", e);
        }

        WfAttachment attachment = new WfAttachment();
        attachment.setFileName(fileName);
        attachment.setFileSuffix(suffix);
        attachment.setFileSize((long) bytes.length);
        attachment.setFileType(contentType);
        attachment.setStorageType(2);
        attachment.setBucketName(bucket);
        attachment.setObjectName(objectName);
        attachment.setStoragePath(objectName);
        attachment.setAccessUrl(minioConfig.getEndpoint() + "/" + bucket + "/" + objectName);
        attachment.setUploadUserId(SecurityUtils.getCurrentUserIdOrNull());
        attachment.setBizType(bizType);
        attachment.setBizId(bizId);
        attachment.setNodeId(nodeId);
        attachment.setCreateTime(LocalDateTime.now());
        wfAttachmentMapper.insert(attachment);
        return attachment;
    }

    public WfAttachment getAttachment(Long id) {
        if (id == null) return null;
        return wfAttachmentMapper.selectById(id);
    }

    public byte[] getBytes(Long attachmentId) {
        WfAttachment att = getAttachment(attachmentId);
        if (att == null) return new byte[0];
        if (att.getObjectName() != null && att.getStorageType() != null && att.getStorageType() == 2) {
            try (InputStream is = minioStorageService.getObject(att.getObjectName())) {
                return is.readAllBytes();
            } catch (Exception e) {
                log.error("读取附件失败 id={}, error={}", attachmentId, e.getMessage());
            }
        }
        return new byte[0];
    }

    public byte[] batchZip(List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return new byte[0];
        List<WfAttachment> attachments = wfAttachmentMapper.selectBatchIds(attachmentIds);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (WfAttachment att : attachments) {
                if (att == null || att.getObjectName() == null) continue;
                try (InputStream is = minioStorageService.getObject(att.getObjectName())) {
                    ZipEntry entry = new ZipEntry(att.getFileName());
                    zos.putNextEntry(entry);
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = is.read(buf)) > 0) zos.write(buf, 0, len);
                    zos.closeEntry();
                } catch (Exception e) {
                    log.warn("跳过附件 {}: {}", att.getId(), e.getMessage());
                }
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("打包下载失败: {}", e.getMessage(), e);
            throw new RuntimeException("打包下载失败", e);
        }
    }

    public String previewUrl(WfAttachment att) {
        if (att == null) return null;
        if (att.getObjectName() != null && att.getStorageType() != null && att.getStorageType() == 2) {
            try {
                return minioStorageService.getPresignedUrl(att.getObjectName(), Method.GET);
            } catch (Exception ignore) {
            }
        }
        return att.getAccessUrl();
    }

    public String previewUrlById(Long id) {
        return previewUrl(getAttachment(id));
    }
}
