package com.innerworkflow.approval.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfEscalationRuleQueryDTO;
import com.innerworkflow.approval.dto.WfEscalationRuleSaveDTO;
import com.innerworkflow.approval.entity.WfEscalationRule;
import com.innerworkflow.approval.mapper.WfEscalationRuleMapper;
import com.innerworkflow.approval.service.WfEscalationRuleService;
import com.innerworkflow.common.context.TenantContext;
import com.innerworkflow.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WfEscalationRuleServiceImpl extends ServiceImpl<WfEscalationRuleMapper, WfEscalationRule> implements WfEscalationRuleService {

    @Override
    public IPage<WfEscalationRule> page(WfEscalationRuleQueryDTO queryDTO) {
        LambdaQueryWrapper<WfEscalationRule> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getRuleName() != null && !queryDTO.getRuleName().isEmpty()) {
            wrapper.like(WfEscalationRule::getRuleName, queryDTO.getRuleName());
        }
        if (queryDTO.getRuleCode() != null && !queryDTO.getRuleCode().isEmpty()) {
            wrapper.eq(WfEscalationRule::getRuleCode, queryDTO.getRuleCode());
        }
        if (queryDTO.getProcessKey() != null && !queryDTO.getProcessKey().isEmpty()) {
            wrapper.and(w -> w.eq(WfEscalationRule::getProcessKey, queryDTO.getProcessKey())
                    .or().isNull(WfEscalationRule::getProcessKey));
        }
        if (queryDTO.getNodeId() != null && !queryDTO.getNodeId().isEmpty()) {
            wrapper.and(w -> w.eq(WfEscalationRule::getNodeId, queryDTO.getNodeId())
                    .or().isNull(WfEscalationRule::getNodeId));
        }
        if (queryDTO.getEscalateLevel() != null) {
            wrapper.eq(WfEscalationRule::getEscalateLevel, queryDTO.getEscalateLevel());
        }
        if (queryDTO.getEscalateType() != null) {
            wrapper.eq(WfEscalationRule::getEscalateType, queryDTO.getEscalateType());
        }
        if (queryDTO.getEnabled() != null) {
            wrapper.eq(WfEscalationRule::getEnabled, queryDTO.getEnabled());
        }

        wrapper.orderByAsc(WfEscalationRule::getSortOrder);
        wrapper.orderByDesc(WfEscalationRule::getCreateTime);

        Page<WfEscalationRule> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return this.page(page, wrapper);
    }

    @Override
    public WfEscalationRule getById(Long id) {
        return super.getById(id);
    }

    @Override
    public boolean save(WfEscalationRuleSaveDTO dto) {
        WfEscalationRule rule = new WfEscalationRule();
        BeanUtils.copyProperties(dto, rule);
        rule.setTenantId(TenantContext.getTenantId());
        rule.setCreateBy(SecurityUtils.getCurrentUserId());
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateBy(SecurityUtils.getCurrentUserId());
        rule.setUpdateTime(LocalDateTime.now());
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        if (rule.getSortOrder() == null) {
            rule.setSortOrder(0);
        }
        return this.save(rule);
    }

    @Override
    public boolean update(WfEscalationRuleSaveDTO dto) {
        WfEscalationRule rule = new WfEscalationRule();
        BeanUtils.copyProperties(dto, rule);
        rule.setUpdateBy(SecurityUtils.getCurrentUserId());
        rule.setUpdateTime(LocalDateTime.now());
        return this.updateById(rule);
    }

    @Override
    public boolean deleteById(Long id) {
        return this.removeById(id);
    }

    @Override
    public List<WfEscalationRule> listByProcessAndNode(String processKey, String nodeId) {
        return filterRulesByPriority(listAllByProcessAndNodeInternal(processKey, nodeId, null));
    }

    @Override
    public List<WfEscalationRule> listEnabledRules(String processKey, String nodeId) {
        return filterRulesByPriority(listAllByProcessAndNodeInternal(processKey, nodeId, 1));
    }

    private List<WfEscalationRule> listAllByProcessAndNodeInternal(String processKey, String nodeId, Integer enabled) {
        LambdaQueryWrapper<WfEscalationRule> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) {
            wrapper.eq(WfEscalationRule::getEnabled, enabled);
        }
        wrapper.and(w -> w.eq(WfEscalationRule::getProcessKey, processKey).or().isNull(WfEscalationRule::getProcessKey));
        wrapper.and(w -> w.eq(WfEscalationRule::getNodeId, nodeId).or().isNull(WfEscalationRule::getNodeId));
        wrapper.orderByAsc(WfEscalationRule::getEscalateLevel);
        wrapper.orderByAsc(WfEscalationRule::getSortOrder);
        return this.list(wrapper);
    }

    private List<WfEscalationRule> filterRulesByPriority(List<WfEscalationRule> allRules) {
        if (allRules == null || allRules.isEmpty()) {
            return allRules;
        }

        Map<Integer, WfEscalationRule> resultMap = new LinkedHashMap<>();

        for (WfEscalationRule rule : allRules) {
            Integer level = rule.getEscalateLevel();
            if (level == null) {
                continue;
            }

            WfEscalationRule existing = resultMap.get(level);
            if (existing == null) {
                resultMap.put(level, rule);
            } else {
                int existingPriority = getPriority(existing);
                int newPriority = getPriority(rule);
                if (newPriority > existingPriority) {
                    resultMap.put(level, rule);
                }
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    private int getPriority(WfEscalationRule rule) {
        boolean hasProcessKey = StrUtil.isNotBlank(rule.getProcessKey());
        boolean hasNodeId = StrUtil.isNotBlank(rule.getNodeId());

        if (hasProcessKey && hasNodeId) {
            return 3;
        } else if (hasProcessKey) {
            return 2;
        } else {
            return 1;
        }
    }
}
