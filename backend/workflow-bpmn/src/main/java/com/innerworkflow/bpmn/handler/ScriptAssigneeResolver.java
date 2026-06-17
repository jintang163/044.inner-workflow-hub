package com.innerworkflow.bpmn.handler;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.common.enums.AssigneeTypeEnum;
import com.innerworkflow.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ScriptAssigneeResolver implements AssigneeResolver {

    private static final String SCRIPT_ENGINE_NAME = "groovy";

    @Override
    public Integer getAssigneeType() {
        return AssigneeTypeEnum.SCRIPT.getCode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Long> resolve(WfNodeConfig nodeConfig, Long startUserId, Long startDeptId) {
        String script = nodeConfig.getAssigneeScript();
        if (script == null || script.isEmpty()) {
            log.warn("脚本审批人配置为空, nodeId={}", nodeConfig.getNodeId());
            return Collections.emptyList();
        }

        try {
            ScriptEngineManager engineManager = new ScriptEngineManager();
            ScriptEngine engine = engineManager.getEngineByName(SCRIPT_ENGINE_NAME);
            if (engine == null) {
                engine = engineManager.getEngineByName("javascript");
            }
            if (engine == null) {
                log.error("脚本引擎不可用, nodeId={}", nodeConfig.getNodeId());
                return Collections.emptyList();
            }

            engine.put("startUserId", startUserId);
            engine.put("startDeptId", startDeptId);
            engine.put("nodeConfig", nodeConfig);

            Object result = engine.eval(script);
            if (result == null) {
                log.warn("脚本执行结果为空, nodeId={}", nodeConfig.getNodeId());
                return Collections.emptyList();
            }

            if (result instanceof List) {
                return (List<Long>) result;
            } else if (result instanceof Long) {
                return Collections.singletonList((Long) result);
            } else if (result instanceof Number) {
                return Collections.singletonList(((Number) result).longValue());
            } else if (result instanceof String) {
                String jsonStr = (String) result;
                if (JsonUtils.isValidJson(jsonStr)) {
                    return JsonUtils.parseList(jsonStr, Long.class);
                }
            }

            log.warn("脚本返回结果类型不支持, resultType={}, nodeId={}",
                    result.getClass().getName(), nodeConfig.getNodeId());
            return Collections.emptyList();

        } catch (ScriptException e) {
            log.error("脚本执行失败, nodeId={}, error={}", nodeConfig.getNodeId(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
