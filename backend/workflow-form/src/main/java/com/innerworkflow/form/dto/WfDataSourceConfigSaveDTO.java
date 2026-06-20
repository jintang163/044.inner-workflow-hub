package com.innerworkflow.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "API数据源配置保存DTO")
public class WfDataSourceConfigSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID(更新时传入)")
    private Long id;

    @NotBlank(message = "数据源编码不能为空")
    @Schema(description = "数据源编码")
    private String sourceCode;

    @NotBlank(message = "数据源名称不能为空")
    @Schema(description = "数据源名称")
    private String sourceName;

    @NotNull(message = "数据源类型不能为空")
    @Schema(description = "数据源类型:1-内部API,2-外部API,3-数据库查询")
    private Integer sourceType;

    @NotBlank(message = "API地址不能为空")
    @Schema(description = "API地址")
    private String apiUrl;

    @Schema(description = "请求方法")
    private String apiMethod;

    @Schema(description = "请求头(JSON)")
    private String apiHeaders;

    @Schema(description = "请求体(JSON)")
    private String apiBody;

    @Schema(description = "请求参数模板(JSON)")
    private String apiParamsTemplate;

    @Schema(description = "响应数据提取路径")
    private String responsePath;

    @Schema(description = "标签字段名")
    private String labelField;

    @Schema(description = "值字段名")
    private String valueField;

    @Schema(description = "子级字段名")
    private String childrenField;

    @Schema(description = "是否启用缓存")
    private Integer cacheEnabled;

    @Schema(description = "缓存过期时间(秒)")
    private Integer cacheTtl;

    @Schema(description = "请求超时时间(毫秒)")
    private Integer timeout;

    @Schema(description = "失败重试次数")
    private Integer retryCount;

    @Schema(description = "认证类型")
    private Integer authType;

    @Schema(description = "认证配置(JSON)")
    private String authConfig;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
