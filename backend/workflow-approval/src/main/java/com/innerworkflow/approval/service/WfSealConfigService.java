package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.form.dto.WfSealConfigSaveDTO;
import com.innerworkflow.form.vo.WfSealConfigVO;

import java.util.List;

public interface WfSealConfigService {

    List<WfSealConfigVO> list(Integer sealType);

    IPage<WfSealConfigVO> page(long current, long size, String keyword, Integer sealType);

    WfSealConfigVO getDetail(Long id);

    WfSealConfigVO save(WfSealConfigSaveDTO dto);

    boolean remove(Long id);
}
