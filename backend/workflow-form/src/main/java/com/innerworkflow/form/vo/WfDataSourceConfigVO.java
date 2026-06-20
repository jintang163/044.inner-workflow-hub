package com.innerworkflow.form.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "API数据源配置VO")
public class WfDataSourceConfigVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "数据源编码")
    private String sourceCode;

    @Schema(description = "数据源名称")
    private String sourceName;

    @Schema(description = "数据源类型:1-内部API,2-外部API,3-数据库查询")
    private Integer sourceType;

    @Schema(description = "API地址")
    private String apiUrl;

    @Schema(description = "请求方法")
    private String apiMethod;

    @Schema(description = "请求参数模板")
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

    @Schema(description = "认证类型:0-无,1-Bearer,2-Basic,3-API Key")
    private Integer authType;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
