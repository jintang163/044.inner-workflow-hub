package com.innerworkflow.form.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_seal_config")
public class WfSealConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sealCode;

    private String sealName;

    private Integer sealType;

    private Long sealOwnerId;

    private String sealOwnerName;

    private Long sealImageId;

    private String sealImageUrl;

    private String sealText;

    private Integer sealShape;

    private Integer sealDiameter;

    private String sealColor;

    private Long digitalCertId;

    private String digitalCertAlias;

    private String certPassword;

    private String signatureAlgorithm;

    private Integer keepCertificate;

    private Integer timestampEnabled;

    private String timestampUrl;

    private String allowedUserIds;

    private String allowedDeptIds;

    private Integer status;

    private String remark;
}
