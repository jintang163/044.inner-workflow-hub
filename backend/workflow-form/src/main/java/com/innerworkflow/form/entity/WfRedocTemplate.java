package com.innerworkflow.form.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_redoc_template")
public class WfRedocTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String templateCode;

    private String templateName;

    private String category;

    private String processKey;

    private Long templateFileId;

    private String templateFileName;

    private String templateStoragePath;

    private String headerColor;

    private Integer headerFontSize;

    private String paperSize;

    private Integer orientation;

    private Double topMargin;

    private Double bottomMargin;

    private Double leftMargin;

    private Double rightMargin;

    private Integer sealEnabled;

    private Long sealId;

    private Integer sealPositionType;

    private Double sealOffsetX;

    private Double sealOffsetY;

    private Double sealScale;

    private Integer signatureEnabled;

    private Long signatureCertId;

    private Integer outputFormat;

    private Integer watermarkEnabled;

    private String watermarkText;

    private String watermarkColor;

    private String placeholderSample;

    private Integer status;

    private String remark;
}
