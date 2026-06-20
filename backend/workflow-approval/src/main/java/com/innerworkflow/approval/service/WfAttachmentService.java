package com.innerworkflow.approval.service;

import com.innerworkflow.approval.entity.WfAttachment;
import com.innerworkflow.approval.entity.WfAttachmentPermission;
import com.innerworkflow.approval.vo.WfAttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface WfAttachmentService {

    WfAttachment getById(Long id);

    WfAttachmentVO upload(MultipartFile file, String bizType, String bizId, String nodeId);

    List<WfAttachmentVO> listByBiz(String bizType, String bizId);

    List<WfAttachmentVO> listByBizWithPermission(String bizType, String bizId, String nodeId);

    List<WfAttachmentVO> listByIds(List<Long> ids);

    boolean removeById(Long id);

    void updateBizId(List<Long> ids, String bizType, String bizId);

    String getPreviewUrl(Long id);

    String getDownloadUrl(Long id);

    byte[] batchDownload(List<Long> ids);

    WfAttachmentPermission getPermission(Long processVersionId, String nodeId);

    WfAttachmentPermission savePermission(WfAttachmentPermission permission);

    void syncPermissionFromNodeConfig(Long processVersionId, String nodeId, Map<String, String> formPermission);
}
