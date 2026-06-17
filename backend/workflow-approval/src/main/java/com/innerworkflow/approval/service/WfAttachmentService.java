package com.innerworkflow.approval.service;

import com.innerworkflow.approval.entity.WfAttachment;
import com.innerworkflow.approval.vo.WfAttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface WfAttachmentService {

    WfAttachment getById(Long id);

    List<WfAttachmentVO> listByBiz(String bizType, String bizId);

    List<WfAttachmentVO> listByIds(List<Long> ids);

    WfAttachmentVO upload(MultipartFile file, String bizType, String bizId);

    boolean removeById(Long id);

    void updateBizId(List<Long> ids, String bizType, String bizId);
}
