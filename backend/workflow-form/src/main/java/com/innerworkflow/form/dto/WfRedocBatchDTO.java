package com.innerworkflow.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "批量操作红头文件DTO")
public class WfRedocBatchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "红头文件生成记录ID列表")
    private List<Long> ids;

    @Schema(description = "审批单号列表(和ids二选一)")
    private List<String> instanceNos;
}
