package com.innerworkflow.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@Schema(description = "红头文件生成DTO")
public class WfRedocGenerateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "审批单号不能为空")
    @Schema(description = "审批单号")
    private String instanceNo;

    @Schema(description = "任务ID")
    private Long taskId;

    @NotNull(message = "模板ID不能为空")
    @Schema(description = "模板ID")
    private Long templateId;

    @NotBlank(message = "文件标题不能为空")
    @Schema(description = "文件标题")
    private String fileTitle;

    @Schema(description = "文号/审批编号")
    private String approvalNo;

    @Schema(description = "发文字号")
    private String fileNo;

    @Schema(description = "输出格式:1-WORD,2-PDF,3-BOTH(不填则用模板配置)")
    private Integer outputFormat;

    @Schema(description = "印章ID(覆盖模板默认印章)")
    private Long sealId;

    @Schema(description = "是否加盖印章(覆盖模板配置)")
    private Integer sealEnabled;

    @Schema(description = "是否启用国密签名(覆盖模板配置)")
    private Integer signatureEnabled;

    @Schema(description = "国密证书印章ID(覆盖模板默认证书)")
    private Long signatureCertId;

    @Schema(description = "自定义占位符值Map(key=占位符名不含{},value=替换值)")
    private Map<String, Object> placeholderValues;
}
