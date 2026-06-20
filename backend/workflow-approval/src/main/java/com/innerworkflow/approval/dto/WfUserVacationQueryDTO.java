package com.innerworkflow.approval.dto;

import com.innerworkflow.common.dto.PageQuery;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfUserVacationQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Integer vacationType;

    private Integer sourceType;

    private Integer vacationStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
