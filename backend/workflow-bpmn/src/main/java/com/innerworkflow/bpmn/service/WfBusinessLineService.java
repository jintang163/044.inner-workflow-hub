package com.innerworkflow.bpmn.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.bpmn.dto.WfBusinessLineQueryDTO;
import com.innerworkflow.bpmn.entity.WfBusinessLine;

import java.util.List;

public interface WfBusinessLineService {

    IPage<WfBusinessLine> page(WfBusinessLineQueryDTO queryDTO);

    List<WfBusinessLine> listAll();

    WfBusinessLine getById(Long id);

    boolean save(WfBusinessLine businessLine);

    boolean updateById(WfBusinessLine businessLine);

    boolean removeById(Long id);
}
