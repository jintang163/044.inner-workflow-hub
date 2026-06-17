package com.innerworkflow.bpmn.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class WfProcessValidateResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean valid;

    private List<ValidateError> errors = new ArrayList<>();

    public void addError(String level, String nodeId, String message) {
        this.errors.add(new ValidateError(level, nodeId, message));
    }

    @Data
    public static class ValidateError implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String level;

        private String nodeId;

        private String message;

        public ValidateError() {
        }

        public ValidateError(String level, String nodeId, String message) {
            this.level = level;
            this.nodeId = nodeId;
            this.message = message;
        }
    }
}
