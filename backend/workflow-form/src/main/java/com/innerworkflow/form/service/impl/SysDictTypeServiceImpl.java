package com.innerworkflow.form.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.form.dto.SysDictTypeSaveDTO;
import com.innerworkflow.form.entity.SysDictData;
import com.innerworkflow.form.entity.SysDictType;
import com.innerworkflow.form.entity.WfDataSourceConfig;
import com.innerworkflow.form.event.DictChangeEvent;
import com.innerworkflow.form.mapper.SysDictDataMapper;
import com.innerworkflow.form.mapper.SysDictTypeMapper;
import com.innerworkflow.form.mapper.WfDataSourceConfigMapper;
import com.innerworkflow.form.service.SysDictTypeService;
import com.innerworkflow.form.util.ApiDataSourceAdapter;
import com.innerworkflow.form.vo.SysDictDataVO;
import com.innerworkflow.form.vo.SysDictTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType>
        implements SysDictTypeService {

    private static final String DICT_CACHE_PREFIX = "dict:data:";
    private static final String DICT_TYPE_CACHE_PREFIX = "dict:type:";

    private final SysDictDataMapper dictDataMapper;
    private final RedissonClient redissonClient;
    private final ApplicationEventPublisher eventPublisher;
    private final ApiDataSourceAdapter apiAdapter;
    private final WfDataSourceConfigMapper dataSourceConfigMapper;

    @Override
    public List<SysDictTypeVO> listAll() {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysDictType::getDictCode);
        List<SysDictType> list = list(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public SysDictTypeVO getDetail(Long id) {
        SysDictType dictType = getById(id);
        if (dictType == null) {
            throw new BusinessException("字典类型不存在");
        }
        SysDictTypeVO vo = convertToVO(dictType);
        List<SysDictDataVO> dataVOList = getDictDataByCode(dictType.getDictCode());
        vo.setItems(dataVOList);
        return vo;
    }

    @Override
    public Boolean saveDictType(SysDictTypeSaveDTO saveDTO) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictType::getDictCode, saveDTO.getDictCode());
        if (count(wrapper) > 0) {
            throw new BusinessException("字典编码已存在");
        }
        SysDictType entity = new SysDictType();
        BeanUtils.copyProperties(saveDTO, entity);
        fillDefaults(entity);
        boolean result = save(entity);
        if (result) {
            evictCache(saveDTO.getDictCode());
            eventPublisher.publishEvent(new DictChangeEvent(this, saveDTO.getDictCode(), "create"));
        }
        return result;
    }

    @Override
    public Boolean updateDictType(SysDictTypeSaveDTO saveDTO) {
        SysDictType entity = getById(saveDTO.getId());
        if (entity == null) {
            throw new BusinessException("字典类型不存在");
        }
        String oldDictCode = entity.getDictCode();
        BeanUtils.copyProperties(saveDTO, entity);
        boolean result = updateById(entity);
        if (result) {
            evictCache(oldDictCode);
            if (!oldDictCode.equals(saveDTO.getDictCode())) {
                evictCache(saveDTO.getDictCode());
            }
            eventPublisher.publishEvent(new DictChangeEvent(this, saveDTO.getDictCode(), "update"));
        }
        return result;
    }

    @Override
    public Boolean deleteDictType(Long id) {
        SysDictType entity = getById(id);
        if (entity == null) {
            throw new BusinessException("字典类型不存在");
        }
        String dictCode = entity.getDictCode();
        boolean result = removeById(id);
        if (result) {
            evictCache(dictCode);
            eventPublisher.publishEvent(new DictChangeEvent(this, dictCode, "delete"));
        }
        return result;
    }

    @Override
    public List<SysDictDataVO> getDictDataByCode(String dictCode) {
        SysDictType dictType = getDictTypeByCode(dictCode);
        if (dictType == null) {
            throw new BusinessException("字典类型不存在: " + dictCode);
        }
        if (dictType.getStatus() != 1) {
            throw new BusinessException("字典类型已禁用: " + dictCode);
        }

        int sourceType = dictType.getSourceType() == null ? 0 : dictType.getSourceType();

        if (dictType.getCacheEnabled() == 1) {
            List<SysDictDataVO> cached = getFromCache(dictCode);
            if (cached != null) {
                return cached;
            }
        }

        List<SysDictDataVO> result;
        switch (sourceType) {
            case 1:
                result = fetchFromApi(dictType);
                break;
            case 2:
                List<SysDictDataVO> staticData = buildCascadeTree(
                        dictDataMapper.selectByDictCode(dictCode), dictCode);
                List<SysDictDataVO> apiData = fetchFromApi(dictType);
                result = mergeDictData(staticData, apiData);
                result = buildCascadeTreeFromVO(result);
                break;
            case 0:
            default:
                result = buildCascadeTree(dictDataMapper.selectByDictCode(dictCode), dictCode);
                break;
        }

        if (dictType.getCacheEnabled() == 1) {
            saveToCache(dictCode, result, dictType.getCacheTtl());
        }

        return result;
    }

    @Override
    public List<SysDictDataVO> getCascadeDictData(String dictCode, String parentValue) {
        SysDictType dictType = getDictTypeByCode(dictCode);
        if (dictType == null) {
            throw new BusinessException("字典类型不存在: " + dictCode);
        }

        if (dictType.getCacheEnabled() == 1) {
            String cacheKey = DICT_CACHE_PREFIX + dictCode + ":parent:" + parentValue;
            RBucket<String> bucket = redissonClient.getBucket(cacheKey);
            String cached = bucket.get();
            if (cached != null) {
                return JSONUtil.toList(cached, SysDictDataVO.class);
            }
        }

        int sourceType = dictType.getSourceType() == null ? 0 : dictType.getSourceType();
        List<SysDictDataVO> result;

        if (sourceType == 1 || sourceType == 2) {
            List<SysDictDataVO> allData = getDictDataByCode(dictCode);
            result = filterByParentValue(allData, parentValue);
        } else {
            List<SysDictData> dataList = dictDataMapper.selectByDictCodeAndParentValue(dictCode, parentValue);
            result = dataList.stream().map(this::convertDataToVO).collect(Collectors.toList());
        }

        if (dictType.getCacheEnabled() == 1) {
            String cacheKey = DICT_CACHE_PREFIX + dictCode + ":parent:" + parentValue;
            RBucket<String> bucket = redissonClient.getBucket(cacheKey);
            bucket.set(JSONUtil.toJsonStr(result), dictType.getCacheTtl(), TimeUnit.SECONDS);
        }

        return result;
    }

    @Override
    public void refreshCache(String dictCode) {
        evictCache(dictCode);
        SysDictType dictType = getDictTypeByCode(dictCode);
        if (dictType != null && dictType.getCacheEnabled() == 1) {
            getDictDataByCode(dictCode);
        }
        log.info("字典缓存已刷新: {}", dictCode);
    }

    @Override
    public void clearAllDictCache() {
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(DICT_CACHE_PREFIX + "*");
        keys.forEach(key -> redissonClient.getBucket(key).delete());
        log.info("所有字典缓存已清除");
    }

    private List<SysDictDataVO> fetchFromApi(SysDictType dictType) {
        WfDataSourceConfig dsConfig = findDataSourceConfig(dictType);

        List<Map<String, Object>> rawItems;
        if (dsConfig != null) {
            rawItems = apiAdapter.fetch(
                    dsConfig.getApiUrl(),
                    dsConfig.getApiMethod(),
                    dsConfig.getApiHeaders(),
                    dsConfig.getApiParamsTemplate(),
                    dsConfig.getApiBody(),
                    dsConfig.getResponsePath(),
                    dsConfig.getLabelField(),
                    dsConfig.getValueField(),
                    dsConfig.getChildrenField(),
                    dsConfig.getTimeout(),
                    dsConfig.getRetryCount(),
                    dsConfig.getAuthType(),
                    dsConfig.getAuthConfig(),
                    new HashMap<>()
            );
        } else {
            rawItems = apiAdapter.fetch(
                    dictType.getApiUrl(),
                    dictType.getApiMethod(),
                    dictType.getApiHeaders(),
                    dictType.getApiParams(),
                    null,
                    dictType.getApiResponsePath(),
                    "label",
                    "value",
                    "children",
                    5000,
                    0,
                    0,
                    null,
                    new HashMap<>()
            );
        }

        List<SysDictDataVO> result = new ArrayList<>();
        String labelField = dsConfig != null ?
                StrUtil.blankToDefault(dsConfig.getLabelField(), "label") : "label";
        String valueField = dsConfig != null ?
                StrUtil.blankToDefault(dsConfig.getValueField(), "value") : "value";
        String childrenField = dsConfig != null ?
                StrUtil.blankToDefault(dsConfig.getChildrenField(), "children") : "children";

        convertRawToDictDataVO(rawItems, dictType.getDictCode(), labelField, valueField, childrenField, result, null);
        return buildCascadeTreeFromVO(result);
    }

    @SuppressWarnings("unchecked")
    private void convertRawToDictDataVO(List<Map<String, Object>> items, String dictCode,
                                        String labelField, String valueField, String childrenField,
                                        List<SysDictDataVO> collector, String parentValue) {
        if (items == null) return;
        int sort = 0;
        for (Map<String, Object> item : items) {
            SysDictDataVO vo = new SysDictDataVO();
            vo.setDictCode(dictCode);
            Object labelObj = item.get(labelField);
            Object valueObj = item.get(valueField);
            vo.setDictLabel(labelObj != null ? labelObj.toString() : "");
            vo.setDictValue(valueObj != null ? valueObj.toString() : "");
            vo.setDictSort(++sort);
            vo.setStatus(1);
            vo.setParentValue(parentValue);
            if (item.get("colorTag") != null) {
                vo.setColorTag(item.get("colorTag").toString());
            }
            collector.add(vo);

            Object children = item.get(childrenField);
            if (children instanceof List) {
                convertRawToDictDataVO((List<Map<String, Object>>) children, dictCode,
                        labelField, valueField, childrenField, collector, vo.getDictValue());
            }
        }
    }

    private WfDataSourceConfig findDataSourceConfig(SysDictType dictType) {
        LambdaQueryWrapper<WfDataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDataSourceConfig::getSourceCode, dictType.getDictCode())
                .eq(WfDataSourceConfig::getStatus, 1)
                .last("LIMIT 1");
        return dataSourceConfigMapper.selectOne(wrapper);
    }

    private List<SysDictDataVO> mergeDictData(List<SysDictDataVO> list1, List<SysDictDataVO> list2) {
        Map<String, SysDictDataVO> map = new LinkedHashMap<>();
        for (SysDictDataVO vo : list1) {
            map.put(vo.getDictValue(), vo);
        }
        for (SysDictDataVO vo : list2) {
            map.put(vo.getDictValue(), vo);
        }
        return new ArrayList<>(map.values());
    }

    private List<SysDictDataVO> filterByParentValue(List<SysDictDataVO> allData, String parentValue) {
        if (allData == null) return new ArrayList<>();
        String targetParent = parentValue == null ? "" : parentValue;
        return allData.stream()
                .filter(vo -> {
                    String pv = vo.getParentValue() == null ? "" : vo.getParentValue();
                    return pv.equals(targetParent);
                })
                .collect(Collectors.toList());
    }

    private SysDictType getDictTypeByCode(String dictCode) {
        String typeCacheKey = DICT_TYPE_CACHE_PREFIX + dictCode;
        RBucket<String> bucket = redissonClient.getBucket(typeCacheKey);
        String cached = bucket.get();
        if (cached != null) {
            return JSONUtil.toBean(cached, SysDictType.class);
        }

        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictType::getDictCode, dictCode);
        SysDictType dictType = getOne(wrapper, false);

        if (dictType != null && dictType.getCacheEnabled() == 1) {
            bucket.set(JSONUtil.toJsonStr(dictType), dictType.getCacheTtl(), TimeUnit.SECONDS);
        }

        return dictType;
    }

    private List<SysDictDataVO> getFromCache(String dictCode) {
        String cacheKey = DICT_CACHE_PREFIX + dictCode;
        RBucket<String> bucket = redissonClient.getBucket(cacheKey);
        String cached = bucket.get();
        if (cached != null) {
            return JSONUtil.toList(cached, SysDictDataVO.class);
        }
        return null;
    }

    private void saveToCache(String dictCode, List<SysDictDataVO> data, Integer ttl) {
        String cacheKey = DICT_CACHE_PREFIX + dictCode;
        RBucket<String> bucket = redissonClient.getBucket(cacheKey);
        bucket.set(JSONUtil.toJsonStr(data), ttl, TimeUnit.SECONDS);
    }

    private void evictCache(String dictCode) {
        String cacheKey = DICT_CACHE_PREFIX + dictCode;
        redissonClient.getBucket(cacheKey).delete();

        String typeCacheKey = DICT_TYPE_CACHE_PREFIX + dictCode;
        redissonClient.getBucket(typeCacheKey).delete();

        Iterable<String> cascadeKeys = redissonClient.getKeys()
                .getKeysByPattern(DICT_CACHE_PREFIX + dictCode + ":parent:*");
        cascadeKeys.forEach(key -> redissonClient.getBucket(key).delete());
    }

    private List<SysDictDataVO> buildCascadeTree(List<SysDictData> dataList, String dictCode) {
        List<SysDictDataVO> allVOs = dataList.stream()
                .map(this::convertDataToVO)
                .collect(Collectors.toList());
        return buildCascadeTreeFromVO(allVOs);
    }

    private List<SysDictDataVO> buildCascadeTreeFromVO(List<SysDictDataVO> allVOs) {
        Map<String, List<SysDictDataVO>> parentMap = allVOs.stream()
                .filter(vo -> vo.getParentValue() != null && !vo.getParentValue().isEmpty())
                .collect(Collectors.groupingBy(SysDictDataVO::getParentValue));

        List<SysDictDataVO> roots = allVOs.stream()
                .filter(vo -> vo.getParentValue() == null || vo.getParentValue().isEmpty())
                .collect(Collectors.toList());

        for (SysDictDataVO root : roots) {
            buildChildren(root, parentMap);
        }

        return roots;
    }

    private void buildChildren(SysDictDataVO parent, Map<String, List<SysDictDataVO>> parentMap) {
        List<SysDictDataVO> children = parentMap.get(parent.getDictValue());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            for (SysDictDataVO child : children) {
                buildChildren(child, parentMap);
            }
        }
    }

    private void fillDefaults(SysDictType entity) {
        if (entity.getCacheEnabled() == null) entity.setCacheEnabled(1);
        if (entity.getCacheTtl() == null) entity.setCacheTtl(3600);
        if (entity.getStatus() == null) entity.setStatus(1);
        if (entity.getSourceType() == null) entity.setSourceType(0);
        if (StrUtil.isBlank(entity.getApiMethod())) entity.setApiMethod("GET");
    }

    private SysDictTypeVO convertToVO(SysDictType entity) {
        SysDictTypeVO vo = new SysDictTypeVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private SysDictDataVO convertDataToVO(SysDictData entity) {
        SysDictDataVO vo = new SysDictDataVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
