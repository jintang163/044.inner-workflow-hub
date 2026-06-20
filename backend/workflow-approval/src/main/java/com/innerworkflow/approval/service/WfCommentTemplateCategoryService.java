package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfCommentTemplateCategoryQueryDTO;
import com.innerworkflow.approval.dto.WfCommentTemplateCategorySaveDTO;
import com.innerworkflow.approval.entity.WfCommentTemplateCategory;
import com.innerworkflow.approval.vo.WfCommentTemplateCategoryVO;

import java.util.List;

public interface WfCommentTemplateCategoryService {

    IPage<WfCommentTemplateCategoryVO> page(WfCommentTemplateCategoryQueryDTO queryDTO);

    WfCommentTemplateCategoryVO getDetail(Long id);

    List<WfCommentTemplateCategoryVO> listByScope(Integer scopeType);

    List<WfCommentTemplateCategoryVO> listAvailable();

    boolean save(WfCommentTemplateCategorySaveDTO saveDTO);

    boolean update(WfCommentTemplateCategorySaveDTO saveDTO);

    boolean delete(Long id);

    boolean updateStatus(Long id, Integer status);

    WfCommentTemplateCategory getEntityById(Long id);
}
