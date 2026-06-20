package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfEscalationRuleQueryDTO;
import com.innerworkflow.approval.dto.WfEscalationRuleSaveDTO;
import com.innerworkflow.approval.entity.WfEscalationRule;

import java.util.List;

public interface WfEscalationRuleService {

    IPage<WfEscalationRule> page(WfEscalationRuleQueryDTO queryDTO);

    WfEscalationRule getById(Long id);

    boolean save(WfEscalationRuleSaveDTO dto);

    boolean update(WfEscalationRuleSaveDTO dto);

    boolean deleteById(Long id);

    List<WfEscalationRule> listByProcessAndNode(String processKey, String nodeId);

    List<WfEscalationRule> listEnabledRules(String processKey, String nodeId);
}
