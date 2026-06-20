package com.innerworkflow.approval.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfAgentConfigQueryDTO;
import com.innerworkflow.approval.dto.WfAgentConfigSaveDTO;
import com.innerworkflow.approval.entity.WfAgentConfig;
import com.innerworkflow.approval.mapper.WfAgentConfigMapper;
import com.innerworkflow.approval.service.WfAgentConfigService;
import com.innerworkflow.common.context.TenantContext;
import com.innerworkflow.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WfAgentConfigServiceImpl extends ServiceImpl<WfAgentConfigMapper, WfAgentConfig> implements WfAgentConfigService {

    @Override
    public IPage<WfAgentConfig> page(WfAgentConfigQueryDTO queryDTO) {
        LambdaQueryWrapper<WfAgentConfig> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByAsc(WfAgentConfig::getPriority);
        wrapper.orderByDesc(WfAgentConfig::getCreateTime);
        Page<WfAgentConfig> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return this.page(page, wrapper);
    }

    @Override
    public WfAgentConfig getById(Long id) {
        return super.getById(id);
    }

    @Override
    public boolean save(WfAgentConfigSaveDTO dto) {
        WfAgentConfig config = new WfAgentConfig();
        BeanUtils.copyProperties(dto, config);
        config.setTenantId(TenantContext.getTenantId());
        config.setCreateBy(SecurityUtils.getCurrentUserId());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateBy(SecurityUtils.getCurrentUserId());
        config.setUpdateTime(LocalDateTime.now());
        if (config.getEnabled() == null) {
            config.setEnabled(1);
        }
        if (config.getPriority() == null) {
            config.setPriority(0);
        }
        return this.save(config);
    }

    @Override
    public boolean update(WfAgentConfigSaveDTO dto) {
        WfAgentConfig config = new WfAgentConfig();
        BeanUtils.copyProperties(dto, config);
        config.setUpdateBy(SecurityUtils.getCurrentUserId());
        config.setUpdateTime(LocalDateTime.now());
        return this.updateById(config);
    }

    @Override
    public boolean deleteById(Long id) {
        return this.removeById(id);
    }

    @Override
    public List<WfAgentConfig> listByUserId(Long userId) {
        LambdaQueryWrapper<WfAgentConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfAgentConfig::getUserId, userId);
        wrapper.orderByAsc(WfAgentConfig::getPriority);
        wrapper.orderByDesc(WfAgentConfig::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<WfAgentConfig> listEnabledByUserId(Long userId, Integer configType) {
        LambdaQueryWrapper<WfAgentConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfAgentConfig::getUserId, userId);
        wrapper.eq(WfAgentConfig::getEnabled, 1);
        if (configType != null) {
            wrapper.eq(WfAgentConfig::getConfigType, configType);
        }
        wrapper.orderByAsc(WfAgentConfig::getPriority);
        wrapper.orderByDesc(WfAgentConfig::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public WfAgentConfig getDefaultAgent(Long userId, Integer configType, String processKey) {
        List<WfAgentConfig> configs = listEnabledByUserId(userId, configType);
        if (configs == null || configs.isEmpty()) {
            return null;
        }

        WfAgentConfig matchedConfig = null;
        WfAgentConfig allProcessConfig = null;

        for (WfAgentConfig config : configs) {
            if (StrUtil.isBlank(config.getProcessKeys())) {
                allProcessConfig = config;
                continue;
            }

            List<String> processKeyList = Arrays.asList(config.getProcessKeys().split(","));
            if (processKeyList.contains(processKey)) {
                matchedConfig = config;
                break;
            }
        }

        return matchedConfig != null ? matchedConfig : allProcessConfig;
    }

    @Override
    public Long getDefaultAgentUserId(Long userId, Integer configType, String processKey) {
        WfAgentConfig config = getDefaultAgent(userId, configType, processKey);
        return config != null ? config.getAgentUserId() : null;
    }

    private LambdaQueryWrapper<WfAgentConfig> buildQueryWrapper(WfAgentConfigQueryDTO queryDTO) {
        LambdaQueryWrapper<WfAgentConfig> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getUserId() != null) {
            wrapper.eq(WfAgentConfig::getUserId, queryDTO.getUserId());
        }
        if (queryDTO.getAgentUserId() != null) {
            wrapper.eq(WfAgentConfig::getAgentUserId, queryDTO.getAgentUserId());
        }
        if (queryDTO.getConfigType() != null) {
            wrapper.eq(WfAgentConfig::getConfigType, queryDTO.getConfigType());
        }
        if (queryDTO.getEnabled() != null) {
            wrapper.eq(WfAgentConfig::getEnabled, queryDTO.getEnabled());
        }

        return wrapper;
    }
}
