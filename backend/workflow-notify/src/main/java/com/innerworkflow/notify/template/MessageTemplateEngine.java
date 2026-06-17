package com.innerworkflow.notify.template;

import cn.hutool.core.util.StrUtil;
import com.innerworkflow.common.util.JsonUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.Map;

@Slf4j
@Component
public class MessageTemplateEngine {

    private final Configuration freemarkerConfig;

    public MessageTemplateEngine() {
        this.freemarkerConfig = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        this.freemarkerConfig.setDefaultEncoding("UTF-8");
        this.freemarkerConfig.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
    }

    public String process(String templateContent, Map<String, Object> params) {
        if (StrUtil.isBlank(templateContent)) {
            return templateContent;
        }
        if (params == null || params.isEmpty()) {
            return templateContent;
        }

        try {
            Template template = new Template("template", templateContent, freemarkerConfig);
            StringWriter writer = new StringWriter();
            template.process(params, writer);
            return writer.toString();
        } catch (Exception e) {
            log.warn("FreeMarker模板解析失败，使用简单字符串替换: {}", e.getMessage());
            return simpleReplace(templateContent, params);
        }
    }

    private String simpleReplace(String template, Map<String, Object> params) {
        if (template == null || params == null || params.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String valueStr = value != null ? value.toString() : "";
            result = result.replace("${" + key + "}", valueStr);
            result = result.replace("{" + key + "}", valueStr);
        }
        return result;
    }

    public String processWithJson(String templateContent, String paramsJson) {
        Map<String, Object> params = null;
        if (StrUtil.isNotBlank(paramsJson)) {
            try {
                params = JsonUtils.parseMap(paramsJson);
            } catch (Exception e) {
                log.warn("解析消息参数JSON失败: {}", e.getMessage());
            }
        }
        return process(templateContent, params);
    }
}
