package com.innerworkflow.form.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.form.dto.WfDataSourceConfigSaveDTO;
import com.innerworkflow.form.entity.WfDataSourceConfig;
import com.innerworkflow.form.mapper.WfDataSourceConfigMapper;
import com.innerworkflow.form.service.WfDataSourceConfigService;
import com.innerworkflow.form.vo.WfDataSourceConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WfDataSourceConfigServiceImpl extends ServiceImpl<WfDataSourceConfigMapper, WfDataSourceConfig>
        implements WfDataSourceConfigService {

    private static final String DATASOURCE_CACHE_PREFIX = "datasource:data:";
    private static final Pattern TEMPLATE_VAR_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    private final RedissonClient redissonClient;

    public WfDataSourceConfigServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public List<WfDataSourceConfigVO> listAll() {
        LambdaQueryWrapper<WfDataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(WfDataSourceConfig::getSourceCode);
        List<WfDataSourceConfig> list = list(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public WfDataSourceConfigVO getDetail(Long id) {
        WfDataSourceConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("数据源配置不存在");
        }
        return convertToVO(config);
    }

    @Override
    public Boolean saveDataSourceConfig(WfDataSourceConfigSaveDTO saveDTO) {
        if (saveDTO.getId() == null) {
            LambdaQueryWrapper<WfDataSourceConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WfDataSourceConfig::getSourceCode, saveDTO.getSourceCode());
            if (count(wrapper) > 0) {
                throw new BusinessException("数据源编码已存在");
            }
            WfDataSourceConfig entity = new WfDataSourceConfig();
            BeanUtils.copyProperties(saveDTO, entity);
            fillDefaults(entity);
            return save(entity);
        } else {
            WfDataSourceConfig entity = getById(saveDTO.getId());
            if (entity == null) {
                throw new BusinessException("数据源配置不存在");
            }
            BeanUtils.copyProperties(saveDTO, entity);
            boolean result = updateById(entity);
            if (result) {
                evictCache(saveDTO.getSourceCode());
            }
            return result;
        }
    }

    @Override
    public Boolean updateDataSourceConfig(WfDataSourceConfigSaveDTO saveDTO) {
        WfDataSourceConfig entity = getById(saveDTO.getId());
        if (entity == null) {
            throw new BusinessException("数据源配置不存在");
        }
        String oldCode = entity.getSourceCode();
        BeanUtils.copyProperties(saveDTO, entity);
        boolean result = updateById(entity);
        if (result) {
            evictCache(oldCode);
            if (!oldCode.equals(saveDTO.getSourceCode())) {
                evictCache(saveDTO.getSourceCode());
            }
        }
        return result;
    }

    @Override
    public Boolean deleteDataSourceConfig(Long id) {
        WfDataSourceConfig entity = getById(id);
        if (entity == null) {
            throw new BusinessException("数据源配置不存在");
        }
        boolean result = removeById(id);
        if (result) {
            evictCache(entity.getSourceCode());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> fetchApiData(String sourceCode, Map<String, Object> params) {
        WfDataSourceConfig config = getBySourceCode(sourceCode);
        if (config == null) {
            throw new BusinessException("数据源配置不存在: " + sourceCode);
        }
        if (config.getStatus() != 1) {
            throw new BusinessException("数据源已禁用: " + sourceCode);
        }

        if (config.getCacheEnabled() == 1) {
            String cacheKey = buildCacheKey(sourceCode, params);
            RBucket<String> bucket = redissonClient.getBucket(cacheKey);
            String cached = bucket.get();
            if (cached != null) {
                return JSONUtil.toList(cached, Map.class);
            }
        }

        List<Map<String, Object>> result = executeApiCall(config, params);

        if (config.getCacheEnabled() == 1) {
            String cacheKey = buildCacheKey(sourceCode, params);
            RBucket<String> bucket = redissonClient.getBucket(cacheKey);
            bucket.set(JSONUtil.toJsonStr(result), config.getCacheTtl(), TimeUnit.SECONDS);
        }

        return result;
    }

    @Override
    public void refreshCache(String sourceCode) {
        evictCache(sourceCode);
        log.info("数据源缓存已刷新: {}", sourceCode);
    }

    private WfDataSourceConfig getBySourceCode(String sourceCode) {
        LambdaQueryWrapper<WfDataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDataSourceConfig::getSourceCode, sourceCode);
        return getOne(wrapper, false);
    }

    private List<Map<String, Object>> executeApiCall(WfDataSourceConfig config, Map<String, Object> params) {
        String url = resolveTemplate(config.getApiUrl(), params);
        String method = StrUtil.blankToDefault(config.getApiMethod(), "GET");
        int timeout = config.getTimeout() != null ? config.getTimeout() : 5000;
        int retryCount = config.getRetryCount() != null ? config.getRetryCount() : 0;

        Map<String, String> headers = new HashMap<>();
        if (StrUtil.isNotBlank(config.getApiHeaders())) {
            try {
                JSONObject headerObj = JSONUtil.parseObj(config.getApiHeaders());
                headerObj.forEach((key, value) -> headers.put(key, String.valueOf(value)));
            } catch (Exception e) {
                log.warn("解析请求头失败: {}", config.getApiHeaders());
            }
        }

        applyAuthHeaders(headers, config);

        Exception lastException = null;
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                String responseBody;
                if ("POST".equalsIgnoreCase(method)) {
                    String body = resolveTemplate(
                            StrUtil.blankToDefault(config.getApiBody(), "{}"), params);
                    HttpRequest request = HttpRequest.post(url)
                            .timeout(timeout)
                            .body(body, "application/json");
                    headers.forEach(request::header);
                    HttpResponse response = request.execute();
                    responseBody = response.body();
                } else {
                    String queryParams = resolveTemplate(
                            StrUtil.blankToDefault(config.getApiParamsTemplate(), ""), params);
                    String fullUrl = url;
                    if (StrUtil.isNotBlank(queryParams)) {
                        fullUrl = url + (url.contains("?") ? "&" : "?") + queryParams;
                    }
                    HttpRequest request = HttpRequest.get(fullUrl).timeout(timeout);
                    headers.forEach(request::header);
                    HttpResponse response = request.execute();
                    responseBody = response.body();
                }

                return parseResponse(responseBody, config);
            } catch (Exception e) {
                lastException = e;
                log.warn("API调用失败(第{}次): {} - {}", attempt + 1, url, e.getMessage());
            }
        }

        throw new BusinessException("API调用失败: " + url + " - " +
                (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    private void applyAuthHeaders(Map<String, String> headers, WfDataSourceConfig config) {
        if (config.getAuthType() == null || config.getAuthType() == 0) {
            return;
        }

        String authConfig = StrUtil.blankToDefault(config.getAuthConfig(), "{}");
        JSONObject authObj = JSONUtil.parseObj(authConfig);

        switch (config.getAuthType()) {
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
    private List<Map<String, Object>> parseResponse(String responseBody, WfDataSourceConfig config) {
        String labelField = StrUtil.blankToDefault(config.getLabelField(), "label");
        String valueField = StrUtil.blankToDefault(config.getValueField(), "value");
        String childrenField = StrUtil.blankToDefault(config.getChildrenField(), "children");

        Object data = JSONUtil.parse(responseBody);

        if (StrUtil.isNotBlank(config.getResponsePath())) {
            String[] paths = config.getResponsePath().split("\\.");
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
            return normalizeItems((JSONArray) data, labelField, valueField, childrenField);
        } else if (data instanceof JSONObject) {
            JSONArray arr = new JSONArray();
            arr.add(data);
            return normalizeItems(arr, labelField, valueField, childrenField);
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

    private String buildCacheKey(String sourceCode, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return DATASOURCE_CACHE_PREFIX + sourceCode;
        }
        return DATASOURCE_CACHE_PREFIX + sourceCode + ":" + params.hashCode();
    }

    private void evictCache(String sourceCode) {
        Iterable<String> keys = redissonClient.getKeys()
                .getKeysByPattern(DATASOURCE_CACHE_PREFIX + sourceCode + "*");
        keys.forEach(key -> redissonClient.getBucket(key).delete());
    }

    private void fillDefaults(WfDataSourceConfig entity) {
        if (entity.getApiMethod() == null) entity.setApiMethod("GET");
        if (entity.getLabelField() == null) entity.setLabelField("label");
        if (entity.getValueField() == null) entity.setValueField("value");
        if (entity.getChildrenField() == null) entity.setChildrenField("children");
        if (entity.getCacheEnabled() == null) entity.setCacheEnabled(1);
        if (entity.getCacheTtl() == null) entity.setCacheTtl(1800);
        if (entity.getTimeout() == null) entity.setTimeout(5000);
        if (entity.getRetryCount() == null) entity.setRetryCount(0);
        if (entity.getAuthType() == null) entity.setAuthType(0);
        if (entity.getStatus() == null) entity.setStatus(1);
    }

    private WfDataSourceConfigVO convertToVO(WfDataSourceConfig entity) {
        WfDataSourceConfigVO vo = new WfDataSourceConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
