package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WfAttachmentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String fileName;

    private String fileSuffix;

    private Long fileSize;

    private String fileType;

    private Integer storageType;

    private String storagePath;

    private String accessUrl;

    private String md5;

    private Long uploadUserId;

    private String uploadUserName;

    private String bizType;

    private String bizId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
