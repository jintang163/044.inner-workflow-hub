package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfCommentTemplateCategoryQueryDTO;
import com.innerworkflow.approval.dto.WfCommentTemplateCategorySaveDTO;
import com.innerworkflow.approval.entity.WfCommentTemplateCategory;
import com.innerworkflow.approval.enums.CommentTemplateScopeEnum;
import com.innerworkflow.approval.mapper.WfCommentTemplateCategoryMapper;
import com.innerworkflow.approval.service.WfCommentTemplateCategoryService;
import com.innerworkflow.approval.vo.WfCommentTemplateCategoryVO;
import com.innerworkflow.auth.entity.SysDepartment;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.service.SysDeptService;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfCommentTemplateCategoryServiceImpl extends ServiceImpl<WfCommentTemplateCategoryMapper, WfCommentTemplateCategory> implements WfCommentTemplateCategoryService {

    private final SysUserService sysUserService;
    private final SysDeptService sysDeptService;

    @Override
    public IPage<WfCommentTemplateCategoryVO> page(WfCommentTemplateCategoryQueryDTO queryDTO) {
        LambdaQueryWrapper<WfCommentTemplateCategory> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByAsc(WfCommentTemplateCategory::getSortOrder)
                .orderByDesc(WfCommentTemplateCategory::getCreateTime);
        return this.page(queryDTO.buildPage("sort_order asc, create_time desc"), wrapper)
                .convert(this::convertToVO);
    }

    @Override
    public WfCommentTemplateCategoryVO getDetail(Long id) {
        WfCommentTemplateCategory entity = getById(id);
        if (entity == null) {
            throw BusinessException.notFound("分类不存在");
        }
        return convertToVO(entity);
    }

    @Override
    public List<WfCommentTemplateCategoryVO> listByScope(Integer scopeType) {
        LambdaQueryWrapper<WfCommentTemplateCategory> wrapper = new LambdaQueryWrapper<>();
        if (scopeType != null) {
            wrapper.eq(WfCommentTemplateCategory::getScopeType, scopeType);
        }
        wrapper.eq(WfCommentTemplateCategory::getStatus, 1);
        wrapper.orderByAsc(WfCommentTemplateCategory::getSortOrder);
        return this.list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WfCommentTemplateCategoryVO> listAvailable() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long currentDeptId = SecurityUtils.getCurrentDeptId();

        LambdaQueryWrapper<WfCommentTemplateCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCommentTemplateCategory::getStatus, 1);
        wrapper.and(w -> w
                .eq(WfCommentTemplateCategory::getScopeType, CommentTemplateScopeEnum.GLOBAL.getCode())
                .or()
                .eq(WfCommentTemplateCategory::getScopeType, CommentTemplateScopeEnum.DEPT_PUBLIC.getCode())
                .eq(WfCommentTemplateCategory::getDeptId, currentDeptId)
                .or()
                .eq(WfCommentTemplateCategory::getScopeType, CommentTemplateScopeEnum.PERSONAL.getCode())
                .eq(WfCommentTemplateCategory::getCreateBy, currentUserId)
        );
        wrapper.orderByAsc(WfCommentTemplateCategory::getSortOrder)
                .orderByDesc(WfCommentTemplateCategory::getCreateTime);
        return this.list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WfCommentTemplateCategorySaveDTO saveDTO) {
        checkPermission(saveDTO.getScopeType(), saveDTO.getDeptId());
        checkCategoryCodeUnique(null, saveDTO.getCategoryCode());

        WfCommentTemplateCategory entity = new WfCommentTemplateCategory();
        BeanUtils.copyProperties(saveDTO, entity);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        boolean result = this.save(entity);
        if (result) {
            log.info("新增意见模板分类成功, id={}, name={}", entity.getId(), entity.getCategoryName());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WfCommentTemplateCategorySaveDTO saveDTO) {
        WfCommentTemplateCategory entity = getById(saveDTO.getId());
        if (entity == null) {
            throw BusinessException.notFound("分类不存在");
        }

        checkPermission(entity.getScopeType(), entity.getDeptId());
        if (saveDTO.getCategoryCode() != null && !saveDTO.getCategoryCode().equals(entity.getCategoryCode())) {
            checkCategoryCodeUnique(entity.getId(), saveDTO.getCategoryCode());
        }

        if (saveDTO.getScopeType() != null && !saveDTO.getScopeType().equals(entity.getScopeType())) {
            checkPermission(saveDTO.getScopeType(), saveDTO.getDeptId());
        }

        BeanUtils.copyProperties(saveDTO, entity);
        boolean result = this.updateById(entity);
        if (result) {
            log.info("更新意见模板分类成功, id={}", entity.getId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        WfCommentTemplateCategory entity = getById(id);
        if (entity == null) {
            throw BusinessException.notFound("分类不存在");
        }

        checkPermission(entity.getScopeType(), entity.getDeptId());

        boolean result = this.removeById(id);
        if (result) {
            log.info("删除意见模板分类成功, id={}", id);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        WfCommentTemplateCategory entity = getById(id);
        if (entity == null) {
            throw BusinessException.notFound("分类不存在");
        }

        checkPermission(entity.getScopeType(), entity.getDeptId());

        entity.setStatus(status);
        boolean result = this.updateById(entity);
        if (result) {
            log.info("更新意见模板分类状态成功, id={}, status={}", id, status);
        }
        return result;
    }

    @Override
    public WfCommentTemplateCategory getEntityById(Long id) {
        return getById(id);
    }

    private LambdaQueryWrapper<WfCommentTemplateCategory> buildQueryWrapper(WfCommentTemplateCategoryQueryDTO queryDTO) {
        LambdaQueryWrapper<WfCommentTemplateCategory> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(queryDTO.getCategoryName())) {
            wrapper.like(WfCommentTemplateCategory::getCategoryName, queryDTO.getCategoryName());
        }
        if (StrUtil.isNotBlank(queryDTO.getCategoryCode())) {
            wrapper.like(WfCommentTemplateCategory::getCategoryCode, queryDTO.getCategoryCode());
        }
        if (queryDTO.getScopeType() != null) {
            wrapper.eq(WfCommentTemplateCategory::getScopeType, queryDTO.getScopeType());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(WfCommentTemplateCategory::getStatus, queryDTO.getStatus());
        }

        return wrapper;
    }

    private void checkCategoryCodeUnique(Long id, String categoryCode) {
        LambdaQueryWrapper<WfCommentTemplateCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCommentTemplateCategory::getCategoryCode, categoryCode);
        if (id != null) {
            wrapper.ne(WfCommentTemplateCategory::getId, id);
        }
        long count = this.count(wrapper);
        if (count > 0) {
            throw BusinessException.paramError("分类编码已存在");
        }
    }

    private void checkPermission(Integer scopeType, Long deptId) {
        if (SecurityUtils.isSuperAdmin()) {
            return;
        }

        CommentTemplateScopeEnum scopeEnum = CommentTemplateScopeEnum.getByCode(scopeType);
        if (scopeEnum == null) {
            throw BusinessException.paramError("适用范围类型错误");
        }

        switch (scopeEnum) {
            case GLOBAL:
                throw BusinessException.forbidden("只有超级管理员可以管理全局模板分类");
            case DEPT_PUBLIC:
                Long currentDeptId = SecurityUtils.getCurrentDeptId();
                if (deptId == null || !deptId.equals(currentDeptId)) {
                    throw BusinessException.forbidden("只能管理本部门的公共模板分类");
                }
                break;
            case PERSONAL:
            default:
                break;
        }
    }

    private WfCommentTemplateCategoryVO convertToVO(WfCommentTemplateCategory entity) {
        WfCommentTemplateCategoryVO vo = new WfCommentTemplateCategoryVO();
        BeanUtils.copyProperties(entity, vo);

        CommentTemplateScopeEnum scopeEnum = CommentTemplateScopeEnum.getByCode(entity.getScopeType());
        if (scopeEnum != null) {
            vo.setScopeTypeName(scopeEnum.getName());
        }

        if (entity.getDeptId() != null) {
            SysDepartment dept = sysDeptService.getById(entity.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }

        if (entity.getCreateBy() != null) {
            SysUser user = sysUserService.getById(entity.getCreateBy());
            if (user != null) {
                vo.setCreateByName(user.getRealName());
            }
        }

        return vo;
    }
}
