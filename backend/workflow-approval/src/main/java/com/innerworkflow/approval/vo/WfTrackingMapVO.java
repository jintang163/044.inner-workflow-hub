package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WfTrackingMapVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long instanceId;

    private String instanceNo;

    private String title;

    private Double averageDuration;

    private List<TrackingNodeVO> nodes;

    private List<TrackingEdgeVO> edges;

    @Data
    public static class TrackingNodeVO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String nodeId;

        private String nodeName;

        private String nodeType;

        private Integer nodeCategory;

        private String status;

        private String statusName;

        private Long duration;

        private Double durationDeviation;

        private Boolean isBottleneck;

        private List<NodeOperatorVO> operators;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;

        private String actionRemark;

        private String signatureUrl;
    }

    @Data
    public static class NodeOperatorVO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;

        private String userName;

        private String userAvatar;

        private String deptName;

        private String action;

        private String actionName;

        private String actionRemark;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime operateTime;

        private Long duration;
    }

    @Data
    public static class TrackingEdgeVO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String sourceId;

        private String targetId;

        private String label;

        private Boolean isActualPath;
    }
}
