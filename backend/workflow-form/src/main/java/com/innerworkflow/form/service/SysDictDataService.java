package com.innerworkflow.form.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.form.dto.SysDictDataSaveDTO;
import com.innerworkflow.form.entity.SysDictData;
import com.innerworkflow.form.vo.SysDictDataVO;

import java.util.List;

public interface SysDictDataService extends IService<SysDictData> {

    List<SysDictDataVO> listByDictCode(String dictCode);

    List<SysDictDataVO> listByDictCodeAndParentValue(String dictCode, String parentValue);

    Boolean saveDictData(SysDictDataSaveDTO saveDTO);

    Boolean updateDictData(SysDictDataSaveDTO saveDTO);

    Boolean deleteDictData(Long id);
}
