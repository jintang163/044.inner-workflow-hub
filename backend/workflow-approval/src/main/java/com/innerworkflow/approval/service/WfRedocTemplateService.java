package com.innerworkflow.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.form.dto.WfRedocGenerateDTO;
import com.innerworkflow.form.dto.WfRedocTemplateSaveDTO;
import com.innerworkflow.form.dto.WfRedocBatchDTO;
import com.innerworkflow.form.vo.WfRedocGeneratedVO;
import com.innerworkflow.form.vo.WfRedocTemplateVO;

import java.util.List;
import java.util.Set;

public interface WfRedocTemplateService {

    List<WfRedocTemplateVO> list(String category, String processKey);

    IPage<WfRedocTemplateVO> page(long current, long size, String keyword, String category);

    WfRedocTemplateVO getDetail(Long id);

    WfRedocTemplateVO save(WfRedocTemplateSaveDTO dto);

    boolean remove(Long id);

    Set<String> extractPlaceholders(Long templateId);

    WfRedocGeneratedVO generate(WfRedocGenerateDTO dto);

    WfRedocGeneratedVO generateWithoutCheck(WfRedocGenerateDTO dto);

    List<WfRedocGeneratedVO> autoGenerateForInstance(String instanceNo);

    List<WfRedocGeneratedVO> listByInstance(String instanceNo);

    IPage<WfRedocGeneratedVO> pageGenerated(long current, long size, String instanceNo, String templateId);

    WfRedocGeneratedVO getGeneratedDetail(Long id);

    boolean markPrinted(Long id);

    boolean markDownloaded(Long id);

    byte[] batchPrintPdf(WfRedocBatchDTO dto);

    byte[] batchDownload(WfRedocBatchDTO dto);
}
