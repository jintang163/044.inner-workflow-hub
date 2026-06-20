package com.innerworkflow.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "电子印章保存DTO")
public class WfSealConfigSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID(更新时传)")
    private Long id;

    @NotBlank(message = "印章编码不能为空")
    @Schema(description = "印章编码")
    private String sealCode;

    @NotBlank(message = "印章名称不能为空")
    @Schema(description = "印章名称")
    private String sealName;

    @Schema(description = "印章类型")
    private Integer sealType;

    @Schema(description = "印章所属单位/部门ID")
    private Long sealOwnerId;

    @Schema(description = "印章所属单位/部门名称")
    private String sealOwnerName;

    @Schema(description = "印章图片文件ID")
    private Long sealImageId;

    @Schema(description = "印章文字(无图片时自动生成)")
    private String sealText;

    @Schema(description = "印章形状")
    private Integer sealShape;

    @Schema(description = "印章直径(毫米)")
    private Integer sealDiameter;

    @Schema(description = "印章颜色")
    private String sealColor;

    @Schema(description = "关联数字证书ID")
    private Long digitalCertId;

    @Schema(description = "签名算法")
    private String signatureAlgorithm;

    @Schema(description = "允许使用的用户ID列表")
    private String allowedUserIds;

    @Schema(description = "允许使用的部门ID列表")
    private String allowedDeptIds;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
