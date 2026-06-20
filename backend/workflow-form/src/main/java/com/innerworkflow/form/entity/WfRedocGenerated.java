package com.innerworkflow.form.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_redoc_generated")
public class WfRedocGenerated {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String instanceNo;

    private String processKey;

    private Long taskId;

    private Long templateId;

    private String templateCode;

    private String templateName;

    private String fileTitle;

    private String approvalNo;

    private String fileNo;

    private Integer outputFormat;

    private Long wordFileId;

    private String wordFileName;

    private Long wordFileSize;

    private Long pdfFileId;

    private String pdfFileName;

    private Long pdfFileSize;

    private Integer sealApplied;

    private Long sealId;

    private Integer signatureApplied;

    private Long signatureCertId;

    private LocalDateTime generateTime;

    private Long generateBy;

    private String generateByName;

    private String placeholderValues;

    private Integer printCount;

    private LocalDateTime lastPrintTime;

    private Long lastPrintBy;

    private Integer downloadCount;

    private LocalDateTime lastDownloadTime;

    private Long lastDownloadBy;

    private Integer status;

    private String remark;

    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;
}
