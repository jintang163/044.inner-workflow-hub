package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfCommentTemplateQueryDTO;
import com.innerworkflow.approval.dto.WfCommentTemplateSaveDTO;
import com.innerworkflow.approval.entity.WfCommentTemplate;
import com.innerworkflow.approval.vo.WfCommentTemplateVO;

import java.util.List;

public interface WfCommentTemplateService {

    IPage<WfCommentTemplateVO> page(WfCommentTemplateQueryDTO queryDTO);

    WfCommentTemplateVO getDetail(Long id);

    List<WfCommentTemplateVO> listByCategoryId(Long categoryId);

    List<WfCommentTemplateVO> listMyAvailable();

    List<WfCommentTemplateVO> listByScopeType(Integer scopeType);

    boolean save(WfCommentTemplateSaveDTO saveDTO);

    boolean update(WfCommentTemplateSaveDTO saveDTO);

    boolean delete(Long id);

    boolean updateStatus(Long id, Integer status);

    boolean incrementUseCount(Long id);

    WfCommentTemplate getEntityById(Long id);
}
