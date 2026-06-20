package com.innerworkflow.approval.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.config.AttachmentConfig;
import com.innerworkflow.approval.config.MinioConfig;
import com.innerworkflow.approval.config.MinioStorageService;
import com.innerworkflow.approval.entity.WfAttachment;
import com.innerworkflow.approval.entity.WfAttachmentPermission;
import com.innerworkflow.approval.mapper.WfAttachmentMapper;
import com.innerworkflow.approval.mapper.WfAttachmentPermissionMapper;
import com.innerworkflow.approval.service.WfAttachmentService;
import com.innerworkflow.approval.vo.WfAttachmentVO;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfAttachmentServiceImpl extends ServiceImpl<WfAttachmentMapper, WfAttachment> implements WfAttachmentService {

    private final MinioStorageService minioStorageService;
    private final MinioConfig minioConfig;
    private final AttachmentConfig attachmentConfig;
    private final WfAttachmentPermissionMapper attachmentPermissionMapper;

    @PostConstruct
    public void init() {
        try {
            minioStorageService.ensureBucket();
            log.info("MinIO storage initialized, bucket={}", minioConfig.getBucketName());
        } catch (Exception e) {
            log.warn("MinIO initialization failed, attachment upload will use local fallback: {}", e.getMessage());
        }
    }

    @Override
    public List<WfAttachmentVO> listByBiz(String bizType, String bizId) {
        if (StrUtil.isBlank(bizType) || StrUtil.isBlank(bizId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<WfAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfAttachment::getBizType, bizType);
        wrapper.eq(WfAttachment::getBizId, bizId);
        wrapper.orderByDesc(WfAttachment::getCreateTime);
        return this.list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WfAttachmentVO> listByBizWithPermission(String bizType, String bizId, Long processVersionId, String currentNodeId) {
        List<WfAttachmentVO> allAttachments = listByBiz(bizType, bizId);
        if (processVersionId == null) {
            return allAttachments;
        }

        LambdaQueryWrapper<WfAttachmentPermission> permWrapper = new LambdaQueryWrapper<>();
        permWrapper.eq(WfAttachmentPermission::getProcessVersionId, processVersionId);
        List<WfAttachmentPermission> permissions = attachmentPermissionMapper.selectList(permWrapper);
        Map<String, WfAttachmentPermission> permMap = permissions.stream()
                .collect(Collectors.toMap(WfAttachmentPermission::getNodeId, p -> p, (a, b) -> a));

        return allAttachments.stream()
                .filter(vo -> {
                    if (StrUtil.isBlank(vo.getNodeId())) {
                        return true;
                    }
                    WfAttachmentPermission perm = permMap.get(vo.getNodeId());
                    if (perm == null) {
                        return true;
                    }
                    return perm.getAttachmentVisible() == null || perm.getAttachmentVisible() == 1;
                })
                .filter(vo -> {
                    if (StrUtil.isBlank(currentNodeId)) {
                        return true;
                    }
                    if (StrUtil.isBlank(vo.getNodeId())) {
                        return true;
                    }
                    return currentNodeId.equals(vo.getNodeId());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<WfAttachmentVO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return baseMapper.selectBatchIds(ids).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public WfAttachmentVO upload(MultipartFile file, String bizType, String bizId, String nodeId) {
        validateFile(file, bizType, bizId);

        String originalFilename = file.getOriginalFilename();
        String fileSuffix = cn.hutool.core.io.FileUtil.getSuffix(originalFilename);
        String uuidName = IdUtil.fastSimpleUUID() + "." + fileSuffix;

        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectName = datePath + "/" + uuidName;

        String bucketName = minioConfig.getBucketName();
        try {
            minioStorageService.putObject(objectName, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (Exception e) {
            log.error("MinIO upload failed, falling back to error: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "文件上传至存储服务失败");
        }

        WfAttachment attachment = new WfAttachment();
        attachment.setFileName(originalFilename);
        attachment.setFileSuffix(fileSuffix);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setStorageType(2);
        attachment.setBucketName(bucketName);
        attachment.setObjectName(objectName);
        attachment.setStoragePath(objectName);
        attachment.setAccessUrl(minioConfig.getEndpoint() + "/" + bucketName + "/" + objectName);
        attachment.setUploadUserId(SecurityUtils.getCurrentUserId());
        attachment.setBizType(bizType);
        attachment.setBizId(bizId);
        attachment.setNodeId(nodeId);

        this.save(attachment);

        return convertToVO(attachment);
    }

    @Override
    public boolean removeById(Long id) {
        WfAttachment attachment = this.getById(id);
        if (attachment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "附件不存在");
        }
        if (attachment.getObjectName() != null && attachment.getStorageType() != null && attachment.getStorageType() == 2) {
            try {
                minioStorageService.removeObject(attachment.getObjectName());
            } catch (Exception e) {
                log.warn("MinIO delete file failed, objectName={}, error={}", attachment.getObjectName(), e.getMessage());
            }
        }
        return super.removeById(id);
    }

    @Override
    public void updateBizId(List<Long> ids, String bizType, String bizId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        LambdaUpdateWrapper<WfAttachment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(WfAttachment::getId, ids);
        wrapper.set(WfAttachment::getBizType, bizType);
        wrapper.set(WfAttachment::getBizId, bizId);
        this.update(wrapper);
    }

    @Override
    public String getPreviewUrl(Long id) {
        WfAttachment attachment = this.getById(id);
        if (attachment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "附件不存在");
        }
        String suffix = attachment.getFileSuffix();
        if (!isPreviewable(suffix)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该文件类型不支持在线预览");
        }
        if (attachment.getObjectName() != null && attachment.getStorageType() != null && attachment.getStorageType() == 2) {
            return minioStorageService.getPresignedUrl(attachment.getObjectName(), Method.GET);
        }
        return attachment.getAccessUrl();
    }

    @Override
    public String getDownloadUrl(Long id) {
        WfAttachment attachment = this.getById(id);
        if (attachment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "附件不存在");
        }
        if (attachment.getObjectName() != null && attachment.getStorageType() != null && attachment.getStorageType() == 2) {
            return minioStorageService.getPresignedUrl(attachment.getObjectName(), Method.GET);
        }
        return attachment.getAccessUrl();
    }

    @Override
    public byte[] batchDownload(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要下载的附件");
        }
        List<WfAttachment> attachments = baseMapper.selectBatchIds(ids);
        if (attachments.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "附件不存在");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (WfAttachment attachment : attachments) {
                if (attachment.getObjectName() != null && attachment.getStorageType() != null && attachment.getStorageType() == 2) {
                    try (InputStream is = minioStorageService.getObject(attachment.getObjectName())) {
                        ZipEntry entry = new ZipEntry(attachment.getFileName());
                        zos.putNextEntry(entry);
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                    } catch (Exception e) {
                        log.error("Download attachment failed, id={}, error={}", attachment.getId(), e.getMessage());
                    }
                }
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Batch download failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "批量下载失败");
        }
    }

    @Override
    public WfAttachmentPermission getPermission(Long processVersionId, String nodeId) {
        LambdaQueryWrapper<WfAttachmentPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfAttachmentPermission::getProcessVersionId, processVersionId);
        wrapper.eq(WfAttachmentPermission::getNodeId, nodeId);
        wrapper.last("LIMIT 1");
        return attachmentPermissionMapper.selectOne(wrapper);
    }

    @Override
    public WfAttachmentPermission savePermission(WfAttachmentPermission permission) {
        WfAttachmentPermission existing = getPermission(permission.getProcessVersionId(), permission.getNodeId());
        if (existing != null) {
            permission.setId(existing.getId());
            attachmentPermissionMapper.updateById(permission);
        } else {
            attachmentPermissionMapper.insert(permission);
        }
        return permission;
    }

    @Override
    public void syncPermissionFromNodeConfig(Long processVersionId, String nodeId, Map<String, String> formPermission) {
        boolean hasHiddenFields = formPermission != null && formPermission.containsValue("hidden");
        WfAttachmentPermission permission = getPermission(processVersionId, nodeId);
        if (permission == null) {
            permission = new WfAttachmentPermission();
            permission.setProcessVersionId(processVersionId);
            permission.setNodeId(nodeId);
            permission.setMaxFileSize(attachmentConfig.getMaxFileSize());
            permission.setAllowedTypes(attachmentConfig.getAllowedTypes());
            permission.setMaxFileCount(attachmentConfig.getMaxFileCount());
        }

        if (hasHiddenFields) {
            permission.setAttachmentVisible(0);
            permission.setAttachmentEditable(0);
        } else {
            permission.setAttachmentVisible(1);
            permission.setAttachmentEditable(1);
        }

        savePermission(permission);
        log.info("Synced attachment permission from node config, processVersionId={}, nodeId={}, visible={}",
                processVersionId, nodeId, permission.getAttachmentVisible());
    }

    private void validateFile(MultipartFile file, String bizType, String bizId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }

        long maxSize = attachmentConfig.getMaxFileSize();
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "文件大小超过限制，最大允许" + (maxSize / 1024 / 1024) + "MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件名不能为空");
        }

        String fileSuffix = cn.hutool.core.io.FileUtil.getSuffix(originalFilename);
        if (StrUtil.isBlank(fileSuffix)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无法识别文件类型");
        }

        String allowedTypes = attachmentConfig.getAllowedTypes();
        Set<String> allowedSet = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (!allowedSet.contains(fileSuffix.toLowerCase())) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "不支持的文件类型: " + fileSuffix + "，允许的类型: " + allowedTypes);
        }

        if (StrUtil.isNotBlank(bizType) && StrUtil.isNotBlank(bizId)) {
            LambdaQueryWrapper<WfAttachment> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(WfAttachment::getBizType, bizType);
            countWrapper.eq(WfAttachment::getBizId, bizId);
            long count = this.count(countWrapper);
            if (count >= attachmentConfig.getMaxFileCount()) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "附件数量已达上限(" + attachmentConfig.getMaxFileCount() + "个)");
            }
        }
    }

    private boolean isPreviewable(String suffix) {
        if (StrUtil.isBlank(suffix)) {
            return false;
        }
        Set<String> previewable = Set.of(
                "jpg", "jpeg", "png", "gif", "bmp", "webp", "pdf"
        );
        return previewable.contains(suffix.toLowerCase());
    }

    private WfAttachmentVO convertToVO(WfAttachment attachment) {
        WfAttachmentVO vo = new WfAttachmentVO();
        BeanUtils.copyProperties(attachment, vo);

        if (attachment.getObjectName() != null && attachment.getStorageType() != null && attachment.getStorageType() == 2) {
            try {
                vo.setPreviewUrl(minioStorageService.getPresignedUrl(attachment.getObjectName(), Method.GET));
                vo.setDownloadUrl(minioStorageService.getPresignedUrl(attachment.getObjectName(), Method.GET));
            } catch (Exception e) {
                log.debug("Generate presigned URL failed, fallback to accessUrl: {}", e.getMessage());
                vo.setPreviewUrl(attachment.getAccessUrl());
                vo.setDownloadUrl(attachment.getAccessUrl());
            }
        } else {
            vo.setPreviewUrl(attachment.getAccessUrl());
            vo.setDownloadUrl(attachment.getAccessUrl());
        }

        vo.setPreviewable(isPreviewable(attachment.getFileSuffix()));
        return vo;
    }
}
