package com.innerworkflow.form.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.form.dto.SysDictDataSaveDTO;
import com.innerworkflow.form.entity.SysDictData;
import com.innerworkflow.form.event.DictChangeEvent;
import com.innerworkflow.form.mapper.SysDictDataMapper;
import com.innerworkflow.form.service.SysDictDataService;
import com.innerworkflow.form.vo.SysDictDataVO;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData>
        implements SysDictDataService {

    private final ApplicationEventPublisher eventPublisher;

    public SysDictDataServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<SysDictDataVO> listByDictCode(String dictCode) {
        List<SysDictData> list = baseMapper.selectByDictCode(dictCode);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<SysDictDataVO> listByDictCodeAndParentValue(String dictCode, String parentValue) {
        List<SysDictData> list = baseMapper.selectByDictCodeAndParentValue(dictCode, parentValue);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public Boolean saveDictData(SysDictDataSaveDTO saveDTO) {
        if (saveDTO.getId() == null) {
            SysDictData entity = new SysDictData();
            BeanUtils.copyProperties(saveDTO, entity);
            boolean result = save(entity);
            if (result) {
                eventPublisher.publishEvent(new DictChangeEvent(this, saveDTO.getDictCode(), "dataCreate"));
            }
            return result;
        } else {
            SysDictData entity = getById(saveDTO.getId());
            if (entity == null) {
                throw new BusinessException("字典数据不存在");
            }
            BeanUtils.copyProperties(saveDTO, entity);
            boolean result = updateById(entity);
            if (result) {
                eventPublisher.publishEvent(new DictChangeEvent(this, saveDTO.getDictCode(), "dataUpdate"));
            }
            return result;
        }
    }

    @Override
    public Boolean updateDictData(SysDictDataSaveDTO saveDTO) {
        SysDictData entity = getById(saveDTO.getId());
        if (entity == null) {
            throw new BusinessException("字典数据不存在");
        }
        String dictCode = entity.getDictCode();
        BeanUtils.copyProperties(saveDTO, entity);
        boolean result = updateById(entity);
        if (result) {
            eventPublisher.publishEvent(new DictChangeEvent(this, dictCode, "dataUpdate"));
        }
        return result;
    }

    @Override
    public Boolean deleteDictData(Long id) {
        SysDictData entity = getById(id);
        if (entity == null) {
            return false;
        }
        String dictCode = entity.getDictCode();
        boolean result = removeById(id);
        if (result) {
            eventPublisher.publishEvent(new DictChangeEvent(this, dictCode, "dataDelete"));
        }
        return result;
    }

    private SysDictDataVO convertToVO(SysDictData entity) {
        SysDictDataVO vo = new SysDictDataVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
