package com.innerworkflow.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.auth.dto.DeptSaveDTO;
import com.innerworkflow.auth.entity.SysDepartment;
import com.innerworkflow.auth.entity.SysRoleDept;
import com.innerworkflow.auth.mapper.SysDeptMapper;
import com.innerworkflow.auth.mapper.SysRoleDeptMapper;
import com.innerworkflow.auth.service.SysDeptService;
import com.innerworkflow.auth.vo.DeptVO;
import com.innerworkflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDepartment> implements SysDeptService {

    private final SysRoleDeptMapper sysRoleDeptMapper;

    @Override
    public List<DeptVO> getDeptTree() {
        List<SysDepartment> allDepts = this.list(new LambdaQueryWrapper<SysDepartment>()
                .orderByAsc(SysDepartment::getSortOrder));
        List<DeptVO> deptVOList = allDepts.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return buildDeptTree(deptVOList, 0L);
    }

    @Override
    public List<DeptVO> getDeptList(String deptName, Integer status) {
        LambdaQueryWrapper<SysDepartment> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(deptName), SysDepartment::getDeptName, deptName)
                .eq(status != null, SysDepartment::getStatus, status)
                .orderByAsc(SysDepartment::getSortOrder);
        List<SysDepartment> list = this.list(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public DeptVO getDeptById(Long id) {
        SysDepartment dept = this.getById(id);
        if (dept == null) {
            throw BusinessException.notFound("部门不存在");
        }
        return convertToVO(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDept(DeptSaveDTO saveDTO) {
        SysDepartment existsDept = this.getOne(new LambdaQueryWrapper<SysDepartment>()
                .eq(SysDepartment::getDeptCode, saveDTO.getDeptCode()));
        if (existsDept != null) {
            throw BusinessException.paramError("部门编码已存在");
        }

        SysDepartment dept = BeanUtil.copyProperties(saveDTO, SysDepartment.class);
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }

        SysDepartment parentDept = null;
        if (dept.getParentId() != 0L) {
            parentDept = this.getById(dept.getParentId());
            if (parentDept == null) {
                throw BusinessException.paramError("父部门不存在");
            }
        }

        String ancestors = parentDept != null
                ? parentDept.getAncestors() + parentDept.getId() + ","
                : "0,";
        dept.setAncestors(ancestors);

        this.save(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(DeptSaveDTO saveDTO) {
        SysDepartment dept = this.getById(saveDTO.getId());
        if (dept == null) {
            throw BusinessException.notFound("部门不存在");
        }

        if (!dept.getDeptCode().equals(saveDTO.getDeptCode())) {
            SysDepartment existsDept = this.getOne(new LambdaQueryWrapper<SysDepartment>()
                    .eq(SysDepartment::getDeptCode, saveDTO.getDeptCode()));
            if (existsDept != null) {
                throw BusinessException.paramError("部门编码已存在");
            }
        }

        if (saveDTO.getParentId() != null && !saveDTO.getParentId().equals(dept.getParentId())) {
            if (saveDTO.getParentId().equals(dept.getId())) {
                throw BusinessException.paramError("不能将自己设为父部门");
            }

            SysDepartment newParent = this.getById(saveDTO.getParentId());
            if (newParent == null) {
                throw BusinessException.paramError("父部门不存在");
            }

            if (newParent.getAncestors().contains("," + dept.getId() + ",")) {
                throw BusinessException.paramError("不能将部门移动到其子部门下");
            }
        }

        BeanUtil.copyProperties(saveDTO, dept);
        this.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long id) {
        long childCount = this.count(new LambdaQueryWrapper<SysDepartment>()
                .eq(SysDepartment::getParentId, id));
        if (childCount > 0) {
            throw BusinessException.paramError("存在子部门，不允许删除");
        }

        this.removeById(id);
        sysRoleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getDeptId, id));
    }

    private List<DeptVO> buildDeptTree(List<DeptVO> depts, Long parentId) {
        List<DeptVO> result = new ArrayList<>();
        for (DeptVO dept : depts) {
            if (dept.getParentId().equals(parentId)) {
                dept.setChildren(buildDeptTree(depts, dept.getId()));
                result.add(dept);
            }
        }
        return result;
    }

    private DeptVO convertToVO(SysDepartment dept) {
        return BeanUtil.copyProperties(dept, DeptVO.class);
    }
}
