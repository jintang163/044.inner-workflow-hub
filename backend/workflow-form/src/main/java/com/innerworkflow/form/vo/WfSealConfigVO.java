package com.innerworkflow.form.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "电子印章配置VO")
public class WfSealConfigVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "印章编码")
    private String sealCode;

    @Schema(description = "印章名称")
    private String sealName;

    @Schema(description = "印章类型")
    private Integer sealType;

    @Schema(description = "印章所属单位/部门名称")
    private String sealOwnerName;

    @Schema(description = "印章图片文件ID")
    private Long sealImageId;

    @Schema(description = "印章图片URL")
    private String sealImageUrl;

    @Schema(description = "印章文字")
    private String sealText;

    @Schema(description = "印章形状:1-圆形,2-椭圆,3-方形")
    private Integer sealShape;

    @Schema(description = "印章直径(毫米)")
    private Integer sealDiameter;

    @Schema(description = "印章颜色")
    private String sealColor;

    @Schema(description = "签名算法")
    private String signatureAlgorithm;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
