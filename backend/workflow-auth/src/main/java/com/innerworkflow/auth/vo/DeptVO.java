package com.innerworkflow.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DeptVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    private String ancestors;

    private String deptName;

    private String deptCode;

    private Integer sortOrder;

    private Long leaderUserId;

    private String leaderName;

    private String phone;

    private String email;

    private Integer status;

    private LocalDateTime createTime;

    private List<DeptVO> children;
}
