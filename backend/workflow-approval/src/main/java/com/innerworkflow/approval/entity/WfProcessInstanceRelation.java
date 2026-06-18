package com.innerworkflow.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(value = "wf_process_instance_relation", autoResultMap = true)
public class WfProcessInstanceRelation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentInstanceId;

    private String parentFlowableInstId;

    private String parentNodeId;

    private Long childInstanceId;

    private String childFlowableInstId;

    private String childProcessKey;

    private String childProcessName;

    private Integer relationType;

    private String callActivityNodeId;

    private String callActivityNodeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
