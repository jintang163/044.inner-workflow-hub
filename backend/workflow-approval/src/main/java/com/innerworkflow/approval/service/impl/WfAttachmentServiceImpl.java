package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.entity.WfAttachment;
import com.innerworkflow.approval.mapper.WfAttachmentMapper;
import com.innerworkflow.approval.service.WfAttachmentService;
import com.innerworkflow.approval.vo.WfAttachmentVO;
import com.innerworkflow.common.util.SecurityUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WfAttachmentServiceImpl extends ServiceImpl<WfAttachmentMapper, WfAttachment> implements WfAttachmentService {

    @Value("${workflow.attachment.upload-path:/data/workflow/attachments}")
    private String uploadPath;

    @Value("${workflow.attachment.access-url-prefix:/attachments}")
    private String accessUrlPrefix;

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
    public List<WfAttachmentVO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return this.listByIds(ids).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public WfAttachmentVO upload(MultipartFile file, String bizType, String bizId) {
        String originalFilename = file.getOriginalFilename();
        String fileSuffix = FileUtil.getSuffix(originalFilename);
        String fileName = IdUtil.fastSimpleUUID() + "." + fileSuffix;

        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = datePath + "/" + fileName;
        String fullPath = uploadPath + "/" + relativePath;

        File destFile = new File(fullPath);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }

        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }

        WfAttachment attachment = new WfAttachment();
        attachment.setFileName(originalFilename);
        attachment.setFileSuffix(fileSuffix);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setStorageType(1);
        attachment.setStoragePath(relativePath);
        attachment.setAccessUrl(accessUrlPrefix + "/" + relativePath);
        attachment.setUploadUserId(SecurityUtils.getCurrentUserId());
        attachment.setBizType(bizType);
        attachment.setBizId(bizId);

        this.save(attachment);

        return convertToVO(attachment);
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

    private WfAttachmentVO convertToVO(WfAttachment attachment) {
        WfAttachmentVO vo = new WfAttachmentVO();
        BeanUtils.copyProperties(attachment, vo);
        return vo;
    }
}
