package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WfMultiInstanceSignVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String nodeId;

    private String nodeName;

    private Integer approveType;

    private String approveTypeName;

    private Integer completionType;

    private String completionTypeName;

    private Integer passPercentage;

    private Boolean vetoEnabled;

    private Integer totalSigners;

    private Integer approvedCount;

    private Integer rejectedCount;

    private Integer pendingCount;

    private String progressText;

    private List<SignerStatusVO> signers;

    @Data
    public static class SignerStatusVO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;

        private String userName;

        private String userAvatar;

        private String deptName;

        private Integer signStatus;

        private String signStatusName;

        private String comment;

        private String signatureUrl;

        private List<Long> attachmentIds;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime assignTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime handleTime;

        private Long duration;
    }
}
