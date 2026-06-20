package com.innerworkflow.form.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.form.dto.WfDataSourceConfigSaveDTO;
import com.innerworkflow.form.entity.WfDataSourceConfig;
import com.innerworkflow.form.vo.WfDataSourceConfigVO;

import java.util.List;
import java.util.Map;

public interface WfDataSourceConfigService extends IService<WfDataSourceConfig> {

    List<WfDataSourceConfigVO> listAll();

    WfDataSourceConfigVO getDetail(Long id);

    Boolean saveDataSourceConfig(WfDataSourceConfigSaveDTO saveDTO);

    Boolean updateDataSourceConfig(WfDataSourceConfigSaveDTO saveDTO);

    Boolean deleteDataSourceConfig(Long id);

    List<Map<String, Object>> fetchApiData(String sourceCode, Map<String, Object> params);

    void refreshCache(String sourceCode);
}
