package com.innerworkflow.form.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "红头模板VO")
public class WfRedocTemplateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板分类")
    private String category;

    @Schema(description = "绑定流程key")
    private String processKey;

    @Schema(description = "Word模板文件ID")
    private Long templateFileId;

    @Schema(description = "模板文件名称")
    private String templateFileName;

    @Schema(description = "红头标题颜色")
    private String headerColor;

    @Schema(description = "红头标题字号")
    private Integer headerFontSize;

    @Schema(description = "纸张规格")
    private String paperSize;

    @Schema(description = "纸张方向:1-纵向,2-横向")
    private Integer orientation;

    @Schema(description = "是否启用印章")
    private Integer sealEnabled;

    @Schema(description = "默认印章ID")
    private Long sealId;

    @Schema(description = "印章位置类型")
    private Integer sealPositionType;

    @Schema(description = "印章缩放比例")
    private Double sealScale;

    @Schema(description = "是否启用数字签名")
    private Integer signatureEnabled;

    @Schema(description = "默认输出格式:1-WORD,2-PDF,3-BOTH")
    private Integer outputFormat;

    @Schema(description = "是否启用水印")
    private Integer watermarkEnabled;

    @Schema(description = "水印文字")
    private String watermarkText;

    @Schema(description = "占位符示例JSON")
    private String placeholderSample;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
