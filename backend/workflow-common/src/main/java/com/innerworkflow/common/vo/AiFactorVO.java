package com.innerworkflow.common.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AiFactorVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String key;

    private String value;

    private Double weight;
}
