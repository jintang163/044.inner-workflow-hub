package com.innerworkflow.form.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "红头文件生成记录VO")
public class WfRedocGeneratedVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "审批单号")
    private String instanceNo;

    @Schema(description = "流程key")
    private String processKey;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "文件标题")
    private String fileTitle;

    @Schema(description = "文号/审批编号")
    private String approvalNo;

    @Schema(description = "发文字号")
    private String fileNo;

    @Schema(description = "输出格式:1-WORD,2-PDF,3-BOTH")
    private Integer outputFormat;

    @Schema(description = "WORD文件ID")
    private Long wordFileId;

    @Schema(description = "WORD文件名")
    private String wordFileName;

    @Schema(description = "WORD预览地址")
    private String wordPreviewUrl;

    @Schema(description = "WORD下载地址")
    private String wordDownloadUrl;

    @Schema(description = "PDF文件ID")
    private Long pdfFileId;

    @Schema(description = "PDF文件名")
    private String pdfFileName;

    @Schema(description = "PDF预览地址")
    private String pdfPreviewUrl;

    @Schema(description = "PDF下载地址")
    private String pdfDownloadUrl;

    @Schema(description = "是否已盖印章")
    private Integer sealApplied;

    @Schema(description = "使用的印章ID")
    private Long sealId;

    @Schema(description = "是否已数字签名")
    private Integer signatureApplied;

    @Schema(description = "生成时间")
    private LocalDateTime generateTime;

    @Schema(description = "生成人ID")
    private Long generateBy;

    @Schema(description = "生成人姓名")
    private String generateByName;

    @Schema(description = "打印次数")
    private Integer printCount;

    @Schema(description = "下载次数")
    private Integer downloadCount;

    @Schema(description = "状态:0-已作废,1-有效")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
