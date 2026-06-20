package com.innerworkflow.form.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.innerworkflow.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ApiDataSourceAdapter {

    private static final Pattern TEMPLATE_VAR_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    public List<Map<String, Object>> fetch(String apiUrl, String apiMethod,
                                            String apiHeaders, String apiParams,
                                            String apiBody, String responsePath,
                                            String labelField, String valueField,
                                            String childrenField,
                                            Integer timeout, Integer retryCount,
                                            Integer authType, String authConfig,
                                            Map<String, Object> params) {
        String url = resolveTemplate(apiUrl, params);
        String method = StrUtil.blankToDefault(apiMethod, "GET");
        int to = timeout != null ? timeout : 5000;
        int retry = retryCount != null ? retryCount : 0;

        Map<String, String> headers = parseHeaders(apiHeaders);
        applyAuthHeaders(headers, authType, authConfig);

        Exception lastException = null;
        for (int attempt = 0; attempt <= retry; attempt++) {
            try {
                String responseBody;
                if ("POST".equalsIgnoreCase(method)) {
                    String body = resolveTemplate(StrUtil.blankToDefault(apiBody, "{}"), params);
                    HttpRequest request = HttpRequest.post(url).timeout(to).body(body, "application/json");
                    headers.forEach(request::header);
                    HttpResponse response = request.execute();
                    responseBody = response.body();
                } else {
                    String queryParams = resolveTemplate(StrUtil.blankToDefault(apiParams, ""), params);
                    String fullUrl = url;
                    if (StrUtil.isNotBlank(queryParams)) {
                        fullUrl = url + (url.contains("?") ? "&" : "?") + queryParams;
                    }
                    HttpRequest request = HttpRequest.get(fullUrl).timeout(to);
                    headers.forEach(request::header);
                    HttpResponse response = request.execute();
                    responseBody = response.body();
                }

                return parseResponse(responseBody, responsePath, labelField, valueField, childrenField);
            } catch (Exception e) {
                lastException = e;
                log.warn("API调用失败(第{}次): {} - {}", attempt + 1, url, e.getMessage());
            }
        }

        throw new BusinessException("API调用失败: " + url + " - " +
                (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    private Map<String, String> parseHeaders(String apiHeaders) {
        Map<String, String> headers = new HashMap<>();
        if (StrUtil.isNotBlank(apiHeaders)) {
            try {
                JSONObject headerObj = JSONUtil.parseObj(apiHeaders);
                headerObj.forEach((key, value) -> headers.put(key, String.valueOf(value)));
            } catch (Exception e) {
                log.warn("解析请求头失败: {}", apiHeaders);
            }
        }
        return headers;
    }

    private void applyAuthHeaders(Map<String, String> headers, Integer authType, String authConfig) {
        if (authType == null || authType == 0) return;

        String authCfg = StrUtil.blankToDefault(authConfig, "{}");
        JSONObject authObj = JSONUtil.parseObj(authCfg);

        switch (authType) {
            case 1:
                headers.put("Authorization", "Bearer " + authObj.getStr("token", ""));
                break;
            case 2:
                String username = authObj.getStr("username", "");
                String password = authObj.getStr("password", "");
                String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
                headers.put("Authorization", "Basic " + encoded);
                break;
            case 3:
                String keyName = authObj.getStr("keyName", "X-API-Key");
                String keyValue = authObj.getStr("keyValue", "");
                String location = authObj.getStr("location", "header");
                if ("header".equals(location)) {
                    headers.put(keyName, keyValue);
                }
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseResponse(String responseBody, String responsePath,
                                                    String labelField, String valueField,
                                                    String childrenField) {
        String lf = StrUtil.blankToDefault(labelField, "label");
        String vf = StrUtil.blankToDefault(valueField, "value");
        String cf = StrUtil.blankToDefault(childrenField, "children");

        Object data = JSONUtil.parse(responseBody);

        if (StrUtil.isNotBlank(responsePath)) {
            String[] paths = responsePath.split("\\.");
            for (String path : paths) {
                if (data instanceof JSONObject) {
                    data = ((JSONObject) data).get(path);
                } else if (data instanceof JSONArray) {
                    int index = Integer.parseInt(path);
                    data = ((JSONArray) data).get(index);
                }
            }
        }

        if (data instanceof JSONArray) {
            return normalizeItems((JSONArray) data, lf, vf, cf);
        } else if (data instanceof JSONObject) {
            JSONArray arr = new JSONArray();
            arr.add(data);
            return normalizeItems(arr, lf, vf, cf);
        }

        throw new BusinessException("无法解析API响应数据");
    }

    private List<Map<String, Object>> normalizeItems(JSONArray items, String labelField,
                                                      String valueField, String childrenField) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("label", item.get(labelField));
            normalized.put("value", item.get(valueField));

            Object children = item.get(childrenField);
            if (children instanceof JSONArray) {
                normalized.put("children", normalizeItems((JSONArray) children, labelField, valueField, childrenField));
            }

            for (String key : item.keySet()) {
                if (!normalized.containsKey(key)) {
                    normalized.put(key, item.get(key));
                }
            }
            result.add(normalized);
        }
        return result;
    }

    private String resolveTemplate(String template, Map<String, Object> params) {
        if (StrUtil.isBlank(template) || params == null || params.isEmpty()) {
            return template;
        }
        Matcher matcher = TEMPLATE_VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = params.get(varName);
            matcher.appendReplacement(sb, value != null ? value.toString() : "");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
