package com.innerworkflow.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "红头模板保存DTO")
public class WfRedocTemplateSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID(更新时传)")
    private Long id;

    @NotBlank(message = "模板编码不能为空")
    @Schema(description = "模板编码")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板分类")
    private String category;

    @Schema(description = "绑定流程key")
    private String processKey;

    @NotNull(message = "模板文件不能为空")
    @Schema(description = "Word模板文件ID")
    private Long templateFileId;

    @Schema(description = "红头标题颜色")
    private String headerColor;

    @Schema(description = "红头标题字号")
    private Integer headerFontSize;

    @Schema(description = "纸张规格")
    private String paperSize;

    @Schema(description = "纸张方向")
    private Integer orientation;

    @Schema(description = "上边距(厘米)")
    private Double topMargin;

    @Schema(description = "下边距(厘米)")
    private Double bottomMargin;

    @Schema(description = "左边距(厘米)")
    private Double leftMargin;

    @Schema(description = "右边距(厘米)")
    private Double rightMargin;

    @Schema(description = "是否启用印章")
    private Integer sealEnabled;

    @Schema(description = "默认印章ID")
    private Long sealId;

    @Schema(description = "印章位置类型")
    private Integer sealPositionType;

    @Schema(description = "印章X偏移(厘米)")
    private Double sealOffsetX;

    @Schema(description = "印章Y偏移(厘米)")
    private Double sealOffsetY;

    @Schema(description = "印章缩放比例")
    private Double sealScale;

    @Schema(description = "是否启用数字签名")
    private Integer signatureEnabled;

    @Schema(description = "默认数字证书印章ID")
    private Long signatureCertId;

    @Schema(description = "是否审批完成自动生成")
    private Integer autoGenerate;

    @Schema(description = "默认输出格式")
    private Integer outputFormat;

    @Schema(description = "是否启用水印")
    private Integer watermarkEnabled;

    @Schema(description = "水印文字")
    private String watermarkText;

    @Schema(description = "水印颜色")
    private String watermarkColor;

    @Schema(description = "占位符示例JSON")
    private String placeholderSample;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
