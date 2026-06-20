package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfCommentTemplateQueryDTO;
import com.innerworkflow.approval.dto.WfCommentTemplateSaveDTO;
import com.innerworkflow.approval.entity.WfCommentTemplate;
import com.innerworkflow.approval.entity.WfCommentTemplateCategory;
import com.innerworkflow.approval.enums.CommentTemplateScopeEnum;
import com.innerworkflow.approval.mapper.WfCommentTemplateMapper;
import com.innerworkflow.approval.service.WfCommentTemplateCategoryService;
import com.innerworkflow.approval.service.WfCommentTemplateService;
import com.innerworkflow.approval.vo.WfCommentTemplateVO;
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
public class WfCommentTemplateServiceImpl extends ServiceImpl<WfCommentTemplateMapper, WfCommentTemplate> implements WfCommentTemplateService {

    private final SysUserService sysUserService;
    private final SysDeptService sysDeptService;
    private final WfCommentTemplateCategoryService categoryService;

    @Override
    public IPage<WfCommentTemplateVO> page(WfCommentTemplateQueryDTO queryDTO) {
        LambdaQueryWrapper<WfCommentTemplate> wrapper = buildQueryWrapper(queryDTO);

        String sortBy = queryDTO.getSortBy();
        if ("useCount".equals(sortBy)) {
            wrapper.orderByDesc(WfCommentTemplate::getUseCount);
        } else {
            wrapper.orderByAsc(WfCommentTemplate::getSortOrder)
                    .orderByDesc(WfCommentTemplate::getCreateTime);
        }

        return this.page(queryDTO.buildPage("sort_order asc, create_time desc"), wrapper)
                .convert(this::convertToVO);
    }

    @Override
    public WfCommentTemplateVO getDetail(Long id) {
        WfCommentTemplate entity = getById(id);
        if (entity == null) {
            throw BusinessException.notFound("模板不存在");
        }
        return convertToVO(entity);
    }

    @Override
    public List<WfCommentTemplateVO> listByCategoryId(Long categoryId) {
        LambdaQueryWrapper<WfCommentTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCommentTemplate::getCategoryId, categoryId);
        wrapper.eq(WfCommentTemplate::getStatus, 1);
        wrapper.orderByAsc(WfCommentTemplate::getSortOrder);
        return this.list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WfCommentTemplateVO> listMyAvailable() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long currentDeptId = SecurityUtils.getCurrentDeptId();

        LambdaQueryWrapper<WfCommentTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCommentTemplate::getStatus, 1);
        wrapper.and(w -> w
                .eq(WfCommentTemplate::getScopeType, CommentTemplateScopeEnum.GLOBAL.getCode())
                .or()
                .eq(WfCommentTemplate::getScopeType, CommentTemplateScopeEnum.DEPT_PUBLIC.getCode())
                .eq(WfCommentTemplate::getDeptId, currentDeptId)
                .or()
                .eq(WfCommentTemplate::getScopeType, CommentTemplateScopeEnum.PERSONAL.getCode())
                .eq(WfCommentTemplate::getCreateBy, currentUserId)
        );
        wrapper.orderByAsc(WfCommentTemplate::getSortOrder)
                .orderByDesc(WfCommentTemplate::getUseCount)
                .orderByDesc(WfCommentTemplate::getCreateTime);

        return this.list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WfCommentTemplateVO> listByScopeType(Integer scopeType) {
        LambdaQueryWrapper<WfCommentTemplate> wrapper = new LambdaQueryWrapper<>();
        if (scopeType != null) {
            wrapper.eq(WfCommentTemplate::getScopeType, scopeType);
        }
        wrapper.eq(WfCommentTemplate::getStatus, 1);
        wrapper.orderByAsc(WfCommentTemplate::getSortOrder);
        return this.list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WfCommentTemplateSaveDTO saveDTO) {
        checkCategoryPermission(saveDTO.getCategoryId());
        checkPermission(saveDTO.getScopeType(), saveDTO.getDeptId());

        WfCommentTemplate entity = new WfCommentTemplate();
        BeanUtils.copyProperties(saveDTO, entity);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getUseCount() == null) {
            entity.setUseCount(0);
        }
        boolean result = this.save(entity);
        if (result) {
            log.info("新增意见模板成功, id={}, name={}", entity.getId(), entity.getTemplateName());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WfCommentTemplateSaveDTO saveDTO) {
        WfCommentTemplate entity = getById(saveDTO.getId());
        if (entity == null) {
            throw BusinessException.notFound("模板不存在");
        }

        checkPermission(entity.getScopeType(), entity.getDeptId());

        if (saveDTO.getCategoryId() != null && !saveDTO.getCategoryId().equals(entity.getCategoryId())) {
            checkCategoryPermission(saveDTO.getCategoryId());
        }

        if (saveDTO.getScopeType() != null && !saveDTO.getScopeType().equals(entity.getScopeType())) {
            checkPermission(saveDTO.getScopeType(), saveDTO.getDeptId());
        }

        BeanUtils.copyProperties(saveDTO, entity);
        boolean result = this.updateById(entity);
        if (result) {
            log.info("更新意见模板成功, id={}", entity.getId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        WfCommentTemplate entity = getById(id);
        if (entity == null) {
            throw BusinessException.notFound("模板不存在");
        }

        checkPermission(entity.getScopeType(), entity.getDeptId());

        boolean result = this.removeById(id);
        if (result) {
            log.info("删除意见模板成功, id={}", id);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        WfCommentTemplate entity = getById(id);
        if (entity == null) {
            throw BusinessException.notFound("模板不存在");
        }

        checkPermission(entity.getScopeType(), entity.getDeptId());

        entity.setStatus(status);
        boolean result = this.updateById(entity);
        if (result) {
            log.info("更新意见模板状态成功, id={}, status={}", id, status);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementUseCount(Long id) {
        int rows = this.baseMapper.incrementUseCount(id);
        return rows > 0;
    }

    @Override
    public WfCommentTemplate getEntityById(Long id) {
        return getById(id);
    }

    private LambdaQueryWrapper<WfCommentTemplate> buildQueryWrapper(WfCommentTemplateQueryDTO queryDTO) {
        LambdaQueryWrapper<WfCommentTemplate> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(WfCommentTemplate::getCategoryId, queryDTO.getCategoryId());
        }
        if (StrUtil.isNotBlank(queryDTO.getTemplateName())) {
            wrapper.like(WfCommentTemplate::getTemplateName, queryDTO.getTemplateName());
        }
        if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
            wrapper.and(w -> w
                    .like(WfCommentTemplate::getTemplateName, queryDTO.getKeyword())
                    .or()
                    .like(WfCommentTemplate::getTemplateContent, queryDTO.getKeyword())
            );
        }
        if (queryDTO.getScopeType() != null) {
            wrapper.eq(WfCommentTemplate::getScopeType, queryDTO.getScopeType());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(WfCommentTemplate::getStatus, queryDTO.getStatus());
        }

        return wrapper;
    }

    private void checkCategoryPermission(Long categoryId) {
        WfCommentTemplateCategory category = categoryService.getEntityById(categoryId);
        if (category == null) {
            throw BusinessException.notFound("分类不存在");
        }
        checkPermission(category.getScopeType(), category.getDeptId());
    }

    private void checkPermission(Integer scopeType, Long deptId) {
        if (SecurityUtils.isSuperAdmin()) {
            return;
        }

        CommentTemplateScopeEnum scopeEnum = CommentTemplateScopeEnum.getByCode(scopeType);
        if (scopeEnum == null) {
            throw BusinessException.paramError("适用范围类型错误");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();

        switch (scopeEnum) {
            case GLOBAL:
                throw BusinessException.forbidden("只有超级管理员可以管理全局模板");
            case DEPT_PUBLIC:
                Long currentDeptId = SecurityUtils.getCurrentDeptId();
                if (deptId == null || !deptId.equals(currentDeptId)) {
                    throw BusinessException.forbidden("只能管理本部门的公共模板");
                }
                break;
            case PERSONAL:
            default:
                break;
        }
    }

    private WfCommentTemplateVO convertToVO(WfCommentTemplate entity) {
        WfCommentTemplateVO vo = new WfCommentTemplateVO();
        BeanUtils.copyProperties(entity, vo);

        CommentTemplateScopeEnum scopeEnum = CommentTemplateScopeEnum.getByCode(entity.getScopeType());
        if (scopeEnum != null) {
            vo.setScopeTypeName(scopeEnum.getName());
        }

        if (entity.getCategoryId() != null) {
            WfCommentTemplateCategory category = categoryService.getEntityById(entity.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
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
