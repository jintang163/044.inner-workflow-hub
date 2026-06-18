package com.innerworkflow.bpmn.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfNodeCcConfigDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<CcConfigItem> nodeStartCc;

    private List<CcConfigItem> nodeCompleteCc;

    @Data
    public static class CcConfigItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String type;

        private Object value;
    }
}
