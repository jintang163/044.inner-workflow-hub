package com.innerworkflow.notify.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.notify.dto.MessageLogQueryDTO;
import com.innerworkflow.notify.entity.WfMessageLog;
import com.innerworkflow.notify.vo.MessageLogVO;

import java.util.List;

public interface WfMessageLogService extends IService<WfMessageLog> {

    Page<MessageLogVO> pageList(MessageLogQueryDTO queryDTO);

    MessageLogVO getDetail(Long id);

    List<WfMessageLog> getPendingRetryList(int limit);

    Boolean retrySend(Long id);

    Boolean markRead(Long id);
}
