package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfAgentConfigQueryDTO;
import com.innerworkflow.approval.dto.WfAgentConfigSaveDTO;
import com.innerworkflow.approval.entity.WfAgentConfig;

import java.util.List;

public interface WfAgentConfigService {

    IPage<WfAgentConfig> page(WfAgentConfigQueryDTO queryDTO);

    WfAgentConfig getById(Long id);

    boolean save(WfAgentConfigSaveDTO dto);

    boolean update(WfAgentConfigSaveDTO dto);

    boolean deleteById(Long id);

    List<WfAgentConfig> listByUserId(Long userId);

    List<WfAgentConfig> listEnabledByUserId(Long userId, Integer configType);

    WfAgentConfig getDefaultAgent(Long userId, Integer configType, String processKey);

    Long getDefaultAgentUserId(Long userId, Integer configType, String processKey);
}
