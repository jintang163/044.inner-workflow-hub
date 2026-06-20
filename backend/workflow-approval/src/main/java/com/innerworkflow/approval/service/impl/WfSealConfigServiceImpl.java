package com.innerworkflow.approval.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.service.WfSealConfigService;
import com.innerworkflow.approval.util.RedocStorageHelper;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.form.dto.WfSealConfigSaveDTO;
import com.innerworkflow.form.entity.WfSealConfig;
import com.innerworkflow.form.mapper.WfSealConfigMapper;
import com.innerworkflow.form.vo.WfSealConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfSealConfigServiceImpl extends ServiceImpl<WfSealConfigMapper, WfSealConfig>
        implements WfSealConfigService {

    private final RedocStorageHelper storageHelper;

    @Override
    public List<WfSealConfigVO> list(Integer sealType) {
        LambdaQueryWrapper<WfSealConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfSealConfig::getStatus, 1);
        if (sealType != null) wrapper.eq(WfSealConfig::getSealType, sealType);
        wrapper.orderByDesc(WfSealConfig::getCreateTime);
        return list(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public IPage<WfSealConfigVO> page(long current, long size, String keyword, Integer sealType) {
        LambdaQueryWrapper<WfSealConfig> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(WfSealConfig::getSealName, keyword)
                    .or().like(WfSealConfig::getSealCode, keyword)
                    .or().like(WfSealConfig::getSealText, keyword));
        }
        if (sealType != null) wrapper.eq(WfSealConfig::getSealType, sealType);
        wrapper.orderByDesc(WfSealConfig::getCreateTime);
        return this.page(new Page<>(current, size), wrapper).convert(this::toVO);
    }

    @Override
    public WfSealConfigVO getDetail(Long id) {
        WfSealConfig s = getById(id);
        if (s == null) throw new BusinessException(ResultCode.NOT_FOUND, "印章配置不存在");
        return toVO(s);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WfSealConfigVO save(WfSealConfigSaveDTO dto) {
        WfSealConfig entity = new WfSealConfig();
        BeanUtils.copyProperties(dto, entity);
        if (dto.getId() == null) {
            entity.setCreateTime(LocalDateTime.now());
            entity.setCreateBy(SecurityUtils.getCurrentUserIdOrNull());
            save(entity);
        } else {
            WfSealConfig exist = getById(dto.getId());
            if (exist == null) throw new BusinessException(ResultCode.NOT_FOUND, "印章配置不存在");
            entity.setUpdateTime(LocalDateTime.now());
            entity.setUpdateBy(SecurityUtils.getCurrentUserIdOrNull());
            updateById(entity);
        }
        return toVO(entity);
    }

    @Override
    public boolean remove(Long id) {
        return removeById(id);
    }

    private WfSealConfigVO toVO(WfSealConfig s) {
        WfSealConfigVO vo = new WfSealConfigVO();
        BeanUtils.copyProperties(s, vo);
        if (s.getSealImageId() != null) {
            byte[] imgBytes = storageHelper.getBytes(s.getSealImageId());
            if (imgBytes != null && imgBytes.length > 0) {
                vo.setSealImageUrl("data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(imgBytes));
            }
        }
        return vo;
    }
}
