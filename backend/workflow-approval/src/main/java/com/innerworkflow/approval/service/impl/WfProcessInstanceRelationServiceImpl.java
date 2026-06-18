package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.entity.WfProcessInstanceRelation;
import com.innerworkflow.approval.mapper.WfProcessInstanceRelationMapper;
import com.innerworkflow.approval.service.WfProcessInstanceRelationService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.vo.WfProcessInstanceRelationVO;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.common.enums.InstanceStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfProcessInstanceRelationServiceImpl
        extends ServiceImpl<WfProcessInstanceRelationMapper, WfProcessInstanceRelation>
        implements WfProcessInstanceRelationService {

    private final WfProcessInstanceService processInstanceService;
    private final SysUserService sysUserService;

    @Override
    public boolean save(WfProcessInstanceRelation relation) {
        return super.save(relation);
    }

    @Override
    public WfProcessInstanceRelation getByChildInstanceId(Long childInstanceId) {
        LambdaQueryWrapper<WfProcessInstanceRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessInstanceRelation::getChildInstanceId, childInstanceId);
        return this.getOne(wrapper, false);
    }

    @Override
    public List<WfProcessInstanceRelationVO> listByParentInstanceId(Long parentInstanceId) {
        LambdaQueryWrapper<WfProcessInstanceRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessInstanceRelation::getParentInstanceId, parentInstanceId);
        wrapper.orderByDesc(WfProcessInstanceRelation::getId);
        List<WfProcessInstanceRelation> relations = this.list(wrapper);
        return convertToVOList(relations);
    }

    @Override
    public List<WfProcessInstanceRelationVO> listByParentInstanceIdAndNodeId(Long parentInstanceId, String nodeId) {
        LambdaQueryWrapper<WfProcessInstanceRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessInstanceRelation::getParentInstanceId, parentInstanceId);
        wrapper.eq(WfProcessInstanceRelation::getCallActivityNodeId, nodeId);
        wrapper.orderByDesc(WfProcessInstanceRelation::getId);
        List<WfProcessInstanceRelation> relations = this.list(wrapper);
        return convertToVOList(relations);
    }

    @Override
    public boolean updateChildInstanceIdByFlowableInstId(String childFlowableInstId, Long childInstanceId) {
        LambdaQueryWrapper<WfProcessInstanceRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessInstanceRelation::getChildFlowableInstId, childFlowableInstId);
        WfProcessInstanceRelation relation = this.getOne(wrapper, false);
        if (relation == null) {
            return false;
        }
        relation.setChildInstanceId(childInstanceId);
        return this.updateById(relation);
    }

    private List<WfProcessInstanceRelationVO> convertToVOList(List<WfProcessInstanceRelation> relations) {
        List<WfProcessInstanceRelationVO> voList = new ArrayList<>();
        for (WfProcessInstanceRelation relation : relations) {
            WfProcessInstanceRelationVO vo = new WfProcessInstanceRelationVO();
            BeanUtils.copyProperties(relation, vo);

            if (relation.getChildInstanceId() != null) {
                WfProcessInstance childInstance = processInstanceService.getById(relation.getChildInstanceId());
                if (childInstance != null) {
                    vo.setChildInstanceNo(childInstance.getInstanceNo());
                    vo.setChildInstanceTitle(childInstance.getTitle());
                    vo.setChildInstanceStatus(childInstance.getInstanceStatus());
                    vo.setChildStartTime(childInstance.getStartTime());
                    vo.setChildEndTime(childInstance.getEndTime());
                    vo.setChildStartUserId(childInstance.getStartUserId());

                    InstanceStatusEnum statusEnum = InstanceStatusEnum.getByCode(childInstance.getInstanceStatus());
                    if (statusEnum != null) {
                        vo.setChildInstanceStatusName(statusEnum.getDesc());
                    }

                    if (childInstance.getStartUserId() != null) {
                        SysUser user = sysUserService.getById(childInstance.getStartUserId());
                        if (user != null) {
                            vo.setChildStartUserName(user.getRealName());
                        }
                    }
                }
            }

            voList.add(vo);
        }
        return voList;
    }
}
