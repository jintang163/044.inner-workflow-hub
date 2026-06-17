package com.innerworkflow.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.auth.dto.DeptSaveDTO;
import com.innerworkflow.auth.entity.SysDepartment;
import com.innerworkflow.auth.vo.DeptVO;

import java.util.List;

public interface SysDeptService extends IService<SysDepartment> {

    List<DeptVO> getDeptTree();

    List<DeptVO> getDeptList(String deptName, Integer status);

    DeptVO getDeptById(Long id);

    void saveDept(DeptSaveDTO saveDTO);

    void updateDept(DeptSaveDTO saveDTO);

    void deleteDept(Long id);
}
