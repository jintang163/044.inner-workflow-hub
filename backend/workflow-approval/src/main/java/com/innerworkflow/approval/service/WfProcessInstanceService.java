package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfMyProcessQueryDTO;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.vo.WfProcessInstanceVO;

public interface WfProcessInstanceService {

    IPage<WfProcessInstanceVO> pageMyProcess(WfMyProcessQueryDTO queryDTO);

    WfProcessInstance getById(Long id);

    WfProcessInstance getByFlowableInstId(String flowableInstId);

    WfProcessInstance getByInstanceNo(String instanceNo);

    boolean save(WfProcessInstance instance);

    boolean updateById(WfProcessInstance instance);

    WfProcessInstanceVO getDetailById(Long id);
}
