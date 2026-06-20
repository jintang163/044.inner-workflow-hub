package com.innerworkflow.form.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.form.dto.SysDictTypeSaveDTO;
import com.innerworkflow.form.entity.SysDictType;
import com.innerworkflow.form.vo.SysDictDataVO;
import com.innerworkflow.form.vo.SysDictTypeVO;

import java.util.List;

public interface SysDictTypeService extends IService<SysDictType> {

    List<SysDictTypeVO> listAll();

    SysDictTypeVO getDetail(Long id);

    Boolean saveDictType(SysDictTypeSaveDTO saveDTO);

    Boolean updateDictType(SysDictTypeSaveDTO saveDTO);

    Boolean deleteDictType(Long id);

    List<SysDictDataVO> getDictDataByCode(String dictCode);

    List<SysDictDataVO> getCascadeDictData(String dictCode, String parentValue);

    void refreshCache(String dictCode);

    void clearAllDictCache();
}
