package com.innerworkflow.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "字典数据保存DTO")
public class SysDictDataSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID(更新时传入)")
    private Long id;

    @NotBlank(message = "字典编码不能为空")
    @Schema(description = "所属字典编码")
    private String dictCode;

    @NotBlank(message = "字典标签不能为空")
    @Schema(description = "字典标签")
    private String dictLabel;

    @NotBlank(message = "字典值不能为空")
    @Schema(description = "字典值")
    private String dictValue;

    @Schema(description = "排序序号")
    private Integer dictSort;

    @Schema(description = "颜色标签")
    private String colorTag;

    @Schema(description = "父级字典值(级联)")
    private String parentValue;

    @Schema(description = "是否默认:0-否,1-是")
    private Integer isDefault;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态:0-禁用,1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
