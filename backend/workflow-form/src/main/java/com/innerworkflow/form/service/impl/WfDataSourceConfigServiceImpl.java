package com.innerworkflow.form.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.form.dto.WfDataSourceConfigSaveDTO;
import com.innerworkflow.form.entity.WfDataSourceConfig;
import com.innerworkflow.form.mapper.WfDataSourceConfigMapper;
import com.innerworkflow.form.service.WfDataSourceConfigService;
import com.innerworkflow.form.util.ApiDataSourceAdapter;
import com.innerworkflow.form.vo.WfDataSourceConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WfDataSourceConfigServiceImpl extends ServiceImpl<WfDataSourceConfigMapper, WfDataSourceConfig>
        implements WfDataSourceConfigService {

    private static final String DATASOURCE_CACHE_PREFIX = "datasource:data:";

    private final RedissonClient redissonClient;
    private final ApiDataSourceAdapter apiAdapter;

    public WfDataSourceConfigServiceImpl(RedissonClient redissonClient, ApiDataSourceAdapter apiAdapter) {
        this.redissonClient = redissonClient;
        this.apiAdapter = apiAdapter;
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
                return toListMap(cached);
            }
        }

        Map<String, Object> safeParams = params != null ? params : new HashMap<>();
        List<Map<String, Object>> result = apiAdapter.fetch(
                config.getApiUrl(),
                config.getApiMethod(),
                config.getApiHeaders(),
                config.getApiParamsTemplate(),
                config.getApiBody(),
                config.getResponsePath(),
                config.getLabelField(),
                config.getValueField(),
                config.getChildrenField(),
                config.getTimeout(),
                config.getRetryCount(),
                config.getAuthType(),
                config.getAuthConfig(),
                safeParams
        );

        if (config.getCacheEnabled() == 1) {
            String cacheKey = buildCacheKey(sourceCode, params);
            RBucket<String> bucket = redissonClient.getBucket(cacheKey);
            bucket.set(cn.hutool.json.JSONUtil.toJsonStr(result), config.getCacheTtl(), TimeUnit.SECONDS);
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toListMap(String json) {
        return cn.hutool.json.JSONUtil.toList(json, Map.class);
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
        if (StrUtil.isBlank(entity.getApiMethod())) entity.setApiMethod("GET");
        if (StrUtil.isBlank(entity.getLabelField())) entity.setLabelField("label");
        if (StrUtil.isBlank(entity.getValueField())) entity.setValueField("value");
        if (StrUtil.isBlank(entity.getChildrenField())) entity.setChildrenField("children");
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
