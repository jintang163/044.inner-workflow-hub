package com.innerworkflow.approval.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class CompatibilityCheckVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean compatible;

    private List<String> errors = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();

    private List<String> infos = new ArrayList<>();

    private NodeCheckResult nodeCheck;

    private VariableCheckResult variableCheck;

    @Data
    public static class NodeCheckResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<String> sourceNodes = new ArrayList<>();
        private List<String> targetNodes = new ArrayList<>();
        private List<String> missingNodes = new ArrayList<>();
        private List<String> matchedNodes = new ArrayList<>();
    }

    @Data
    public static class VariableCheckResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private List<String> sourceVariables = new ArrayList<>();
        private List<String> targetVariables = new ArrayList<>();
        private List<String> missingVariables = new ArrayList<>();
        private List<String> matchedVariables = new ArrayList<>();
    }
}
