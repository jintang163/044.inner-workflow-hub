package com.innerworkflow.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.notify.dto.MessageLogQueryDTO;
import com.innerworkflow.notify.entity.WfMessageLog;
import com.innerworkflow.notify.enums.SendStatusEnum;
import com.innerworkflow.notify.mapper.WfMessageLogMapper;
import com.innerworkflow.notify.service.WfMessageLogService;
import com.innerworkflow.notify.service.WfNotifyService;
import com.innerworkflow.notify.vo.MessageLogVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfMessageLogServiceImpl extends ServiceImpl<WfMessageLogMapper, WfMessageLog>
        implements WfMessageLogService {

    @Autowired
    @Lazy
    private WfNotifyService notifyService;

    @Override
    public Page<MessageLogVO> pageList(MessageLogQueryDTO queryDTO) {
        LambdaQueryWrapper<WfMessageLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(queryDTO.getTemplateCode()),
                WfMessageLog::getTemplateCode, queryDTO.getTemplateCode());
        wrapper.eq(StrUtil.isNotBlank(queryDTO.getBusinessType()),
                WfMessageLog::getBusinessType, queryDTO.getBusinessType());
        wrapper.eq(queryDTO.getInstanceId() != null,
                WfMessageLog::getInstanceId, queryDTO.getInstanceId());
        wrapper.eq(queryDTO.getTaskId() != null,
                WfMessageLog::getTaskId, queryDTO.getTaskId());
        wrapper.eq(StrUtil.isNotBlank(queryDTO.getChannelType()),
                WfMessageLog::getChannelType, queryDTO.getChannelType());
        wrapper.eq(queryDTO.getReceiverUserId() != null,
                WfMessageLog::getReceiverUserId, queryDTO.getReceiverUserId());
        wrapper.eq(queryDTO.getSendStatus() != null,
                WfMessageLog::getSendStatus, queryDTO.getSendStatus());
        wrapper.ge(queryDTO.getStartTime() != null,
                WfMessageLog::getCreateTime, queryDTO.getStartTime());
        wrapper.le(queryDTO.getEndTime() != null,
                WfMessageLog::getCreateTime, queryDTO.getEndTime());
        wrapper.orderByDesc(WfMessageLog::getCreateTime);

        Page<WfMessageLog> page = page(queryDTO.buildPage("create_time desc"), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    public MessageLogVO getDetail(Long id) {
        WfMessageLog log = getById(id);
        if (log == null) {
            throw new BusinessException("消息记录不存在");
        }
        return convertToVO(log);
    }

    @Override
    public List<WfMessageLog> getPendingRetryList(int limit) {
        LambdaQueryWrapper<WfMessageLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfMessageLog::getSendStatus, SendStatusEnum.FAILED.getCode());
        wrapper.apply("retry_count < max_retry");
        wrapper.orderByAsc(WfMessageLog::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return list(wrapper);
    }

    @Override
    public Boolean retrySend(Long id) {
        WfMessageLog log = getById(id);
        if (log == null) {
            throw new BusinessException("消息记录不存在");
        }
        if (log.getRetryCount() >= log.getMaxRetry()) {
            throw new BusinessException("已达最大重试次数");
        }
        notifyService.processMessageLog(log);
        return true;
    }

    @Override
    public Boolean markRead(Long id) {
        LambdaUpdateWrapper<WfMessageLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(WfMessageLog::getId, id);
        wrapper.set(WfMessageLog::getIsRead, 1);
        return update(wrapper);
    }

    private MessageLogVO convertToVO(WfMessageLog entity) {
        MessageLogVO vo = new MessageLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
