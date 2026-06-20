package com.innerworkflow.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "字典类型保存DTO")
public class SysDictTypeSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID(更新时传入)")
    private Long id;

    @NotBlank(message = "字典名称不能为空")
    @Schema(description = "字典名称")
    private String dictName;

    @NotBlank(message = "字典编码不能为空")
    @Schema(description = "字典编码")
    private String dictCode;

    @NotNull(message = "数据来源不能为空")
    @Schema(description = "数据来源:0-字典表静态数据,1-API动态获取,2-混合")
    private Integer sourceType;

    @Schema(description = "API数据源地址")
    private String apiUrl;

    @Schema(description = "API请求方法")
    private String apiMethod;

    @Schema(description = "API请求头(JSON)")
    private String apiHeaders;

    @Schema(description = "API请求参数(JSON)")
    private String apiParams;

    @Schema(description = "API响应数据提取路径")
    private String apiResponsePath;

    @Schema(description = "级联关联字段")
    private String cascadeField;

    @Schema(description = "父级字典编码")
    private String cascadeParent;

    @Schema(description = "是否启用缓存")
    private Integer cacheEnabled;

    @Schema(description = "缓存过期时间(秒)")
    private Integer cacheTtl;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
