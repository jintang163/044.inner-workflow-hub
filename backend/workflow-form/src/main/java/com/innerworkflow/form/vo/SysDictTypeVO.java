package com.innerworkflow.form.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "字典类型VO")
public class SysDictTypeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典编码")
    private String dictCode;

    @Schema(description = "数据来源:0-字典表静态数据,1-API动态获取,2-混合")
    private Integer sourceType;

    @Schema(description = "API数据源地址")
    private String apiUrl;

    @Schema(description = "API请求方法")
    private String apiMethod;

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

    @Schema(description = "状态:0-禁用,1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "字典数据项列表")
    private List<SysDictDataVO> items;
}
