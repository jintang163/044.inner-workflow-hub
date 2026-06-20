package com.innerworkflow.form.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_data_source_config")
public class WfDataSourceConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sourceCode;

    private String sourceName;

    private Integer sourceType;

    private String apiUrl;

    private String apiMethod;

    private String apiHeaders;

    private String apiBody;

    private String apiParamsTemplate;

    private String responsePath;

    private String labelField;

    private String valueField;

    private String childrenField;

    private Integer cacheEnabled;

    private Integer cacheTtl;

    private Integer timeout;

    private Integer retryCount;

    private Integer authType;

    private String authConfig;

    private Integer status;

    private String remark;
}
