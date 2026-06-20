package com.innerworkflow.approval.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.entity.WfAttachment;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.service.WfRedocTemplateService;
import com.innerworkflow.approval.util.RedocStorageHelper;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.form.dto.WfRedocBatchDTO;
import com.innerworkflow.form.dto.WfRedocGenerateDTO;
import com.innerworkflow.form.dto.WfRedocTemplateSaveDTO;
import com.innerworkflow.form.entity.WfRedocGenerated;
import com.innerworkflow.form.entity.WfRedocTemplate;
import com.innerworkflow.form.entity.WfSealConfig;
import com.innerworkflow.form.mapper.WfRedocGeneratedMapper;
import com.innerworkflow.form.mapper.WfRedocTemplateMapper;
import com.innerworkflow.form.mapper.WfSealConfigMapper;
import com.innerworkflow.form.util.SealAndSignatureUtil;
import com.innerworkflow.form.util.WordTemplateEngine;
import com.innerworkflow.form.util.WordToPdfConverter;
import com.innerworkflow.form.vo.WfRedocGeneratedVO;
import com.innerworkflow.form.vo.WfRedocTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfRedocTemplateServiceImpl extends ServiceImpl<WfRedocTemplateMapper, WfRedocTemplate>
        implements WfRedocTemplateService {

    private final WfRedocGeneratedMapper generatedMapper;
    private final WfSealConfigMapper sealConfigMapper;
    private final WordTemplateEngine wordTemplateEngine;
    private final WordToPdfConverter wordToPdfConverter;
    private final SealAndSignatureUtil sealAndSignatureUtil;
    private final RedocStorageHelper storageHelper;
    private final WfProcessInstanceService processInstanceService;

    @Override
    public List<WfRedocTemplateVO> list(String category, String processKey) {
        LambdaQueryWrapper<WfRedocTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfRedocTemplate::getStatus, 1);
        if (StrUtil.isNotBlank(category)) wrapper.eq(WfRedocTemplate::getCategory, category);
        if (StrUtil.isNotBlank(processKey)) wrapper.eq(WfRedocTemplate::getProcessKey, processKey);
        wrapper.orderByDesc(WfRedocTemplate::getCreateTime);
        return list(wrapper).stream().map(this::toTemplateVO).collect(Collectors.toList());
    }

    @Override
    public IPage<WfRedocTemplateVO> page(long current, long size, String keyword, String category) {
        LambdaQueryWrapper<WfRedocTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(WfRedocTemplate::getTemplateName, keyword)
                    .or().like(WfRedocTemplate::getTemplateCode, keyword));
        }
        if (StrUtil.isNotBlank(category)) wrapper.eq(WfRedocTemplate::getCategory, category);
        wrapper.orderByDesc(WfRedocTemplate::getCreateTime);
        return this.page(new Page<>(current, size), wrapper).convert(this::toTemplateVO);
    }

    @Override
    public WfRedocTemplateVO getDetail(Long id) {
        WfRedocTemplate t = getById(id);
        if (t == null) throw new BusinessException(ResultCode.NOT_FOUND, "模板不存在");
        return toTemplateVO(t);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WfRedocTemplateVO save(WfRedocTemplateSaveDTO dto) {
        WfRedocTemplate entity = new WfRedocTemplate();
        BeanUtils.copyProperties(dto, entity);
        if (dto.getId() == null) {
            entity.setCreateTime(LocalDateTime.now());
            entity.setCreateBy(SecurityUtils.getCurrentUserIdOrNull());
            save(entity);
        } else {
            WfRedocTemplate exist = getById(dto.getId());
            if (exist == null) throw new BusinessException(ResultCode.NOT_FOUND, "模板不存在");
            entity.setUpdateTime(LocalDateTime.now());
            entity.setUpdateBy(SecurityUtils.getCurrentUserIdOrNull());
            updateById(entity);
        }
        return toTemplateVO(entity);
    }

    @Override
    public boolean remove(Long id) {
        return removeById(id);
    }

    @Override
    public Set<String> extractPlaceholders(Long templateId) {
        WfRedocTemplate t = getById(templateId);
        if (t == null || t.getTemplateFileId() == null) return Collections.emptySet();
        byte[] bytes = storageHelper.getBytes(t.getTemplateFileId());
        return wordTemplateEngine.extractPlaceholders(bytes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WfRedocGeneratedVO generate(WfRedocGenerateDTO dto) {
        WfRedocTemplate template = getById(dto.getTemplateId());
        if (template == null) throw new BusinessException(ResultCode.NOT_FOUND, "模板不存在");
        if (template.getStatus() != null && template.getStatus() == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "模板已禁用");
        }

        WfProcessInstance instance = null;
        if (StrUtil.isNotBlank(dto.getInstanceNo())) {
            instance = processInstanceService.getByInstanceNo(dto.getInstanceNo());
        }

        int outputFormat = dto.getOutputFormat() != null ? dto.getOutputFormat()
                : (template.getOutputFormat() != null ? template.getOutputFormat() : 2);

        Map<String, Object> placeholders = buildPlaceholderValues(dto, template, instance);

        byte[] templateBytes = storageHelper.getBytes(template.getTemplateFileId());
        if (templateBytes == null || templateBytes.length == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "模板文件为空或不存在");
        }
        byte[] processedDocx = wordTemplateEngine.process(templateBytes, placeholders);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String baseName = (dto.getFileTitle() == null ? "红头文件" : dto.getFileTitle()) + "_" + timestamp;

        WfAttachment wordAtt = null;
        if (outputFormat == 1 || outputFormat == 3) {
            wordAtt = storageHelper.saveFromBytes(processedDocx,
                    baseName + ".docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "redoc", dto.getInstanceNo(), null);
        }

        WfAttachment pdfAtt = null;
        byte[] pdfBytes = null;
        if (outputFormat == 2 || outputFormat == 3) {
            pdfBytes = wordToPdfConverter.convertDocxToPdf(processedDocx);

            Integer sealEnabled = dto.getSealEnabled() != null ? dto.getSealEnabled() : template.getSealEnabled();
            Long sealId = dto.getSealId() != null ? dto.getSealId() : template.getSealId();
            if (sealEnabled != null && sealEnabled == 1 && sealId != null) {
                WfSealConfig seal = sealConfigMapper.selectById(sealId);
                if (seal != null && seal.getSealImageId() != null) {
                    byte[] sealImg = storageHelper.getBytes(seal.getSealImageId());
                    if (sealImg != null && sealImg.length > 0) {
                        pdfBytes = sealAndSignatureUtil.applySealImage(pdfBytes, sealImg,
                                template.getSealPositionType(),
                                template.getSealOffsetX(), template.getSealOffsetY(),
                                template.getSealScale());
                    }
                }
            }

            if (template.getWatermarkEnabled() != null && template.getWatermarkEnabled() == 1
                    && StrUtil.isNotBlank(template.getWatermarkText())) {
                pdfBytes = wordToPdfConverter.addWatermark(pdfBytes,
                        template.getWatermarkText(), template.getWatermarkColor());
            }

            pdfAtt = storageHelper.saveFromBytes(pdfBytes, baseName + ".pdf",
                    "application/pdf", "redoc", dto.getInstanceNo(), null);
        }

        WfRedocGenerated g = new WfRedocGenerated();
        g.setInstanceNo(dto.getInstanceNo());
        g.setTaskId(dto.getTaskId());
        g.setTemplateId(template.getId());
        g.setTemplateCode(template.getTemplateCode());
        g.setTemplateName(template.getTemplateName());
        g.setFileTitle(dto.getFileTitle());
        g.setApprovalNo(dto.getApprovalNo());
        g.setFileNo(dto.getFileNo());
        g.setOutputFormat(outputFormat);
        if (wordAtt != null) {
            g.setWordFileId(wordAtt.getId());
            g.setWordFileName(wordAtt.getFileName());
            g.setWordFileSize(wordAtt.getFileSize());
        }
        if (pdfAtt != null) {
            g.setPdfFileId(pdfAtt.getId());
            g.setPdfFileName(pdfAtt.getFileName());
            g.setPdfFileSize(pdfAtt.getFileSize());
        }
        Integer sealEnabled = dto.getSealEnabled() != null ? dto.getSealEnabled() : template.getSealEnabled();
        Long sealId = dto.getSealId() != null ? dto.getSealId() : template.getSealId();
        g.setSealApplied(sealEnabled != null && sealEnabled == 1 && sealId != null ? 1 : 0);
        g.setSealId(sealId);
        g.setSignatureApplied(template.getSignatureEnabled() != null && template.getSignatureEnabled() == 1 ? 1 : 0);
        g.setSignatureCertId(template.getSignatureCertId());
        g.setGenerateTime(LocalDateTime.now());
        g.setGenerateBy(SecurityUtils.getCurrentUserIdOrNull());
        g.setGenerateByName(SecurityUtils.getCurrentUserOpt().map(u -> u.getRealName() != null ? u.getRealName() : u.getUsername()).orElse(null));
        g.setPlaceholderValues(JSONUtil.toJsonStr(placeholders));
        g.setPrintCount(0);
        g.setDownloadCount(0);
        g.setStatus(1);
        g.setCreateTime(LocalDateTime.now());
        g.setUpdateTime(LocalDateTime.now());
        g.setIsDeleted(0);
        if (instance != null) g.setProcessKey(instance.getProcessKey());
        generatedMapper.insert(g);

        return toGeneratedVO(g, wordAtt, pdfAtt);
    }

    private Map<String, Object> buildPlaceholderValues(WfRedocGenerateDTO dto,
                                                        WfRedocTemplate template,
                                                        WfProcessInstance instance) {
        Map<String, Object> values = new HashMap<>();
        values.put("title", dto.getFileTitle());
        values.put("fileTitle", dto.getFileTitle());
        values.put("approvalNo", dto.getApprovalNo() == null ? "" : dto.getApprovalNo());
        values.put("fileNo", dto.getFileNo() == null ? "" : dto.getFileNo());
        values.put("instanceNo", dto.getInstanceNo() == null ? "" : dto.getInstanceNo());
        values.put("generateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        values.put("generateDateShort", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        values.put("generateYear", String.valueOf(LocalDateTime.now().getYear()));
        values.put("generateMonth", String.valueOf(LocalDateTime.now().getMonthValue()));
        values.put("generateDay", String.valueOf(LocalDateTime.now().getDayOfMonth()));
        values.put("currentUser", SecurityUtils.getCurrentUserOpt()
                .map(u -> u.getRealName() != null ? u.getRealName() : u.getUsername()).orElse(""));
        values.put("currentDept", SecurityUtils.getCurrentUserOpt().map(u -> u.getDeptName() == null ? "" : u.getDeptName()).orElse(""));
        if (instance != null) {
            values.put("processKey", instance.getProcessKey() == null ? "" : instance.getProcessKey());
            values.put("processName", instance.getTitle() == null ? "" : instance.getTitle());
            values.put("applicant", instance.getStartUserName() == null ? "" : instance.getStartUserName());
            values.put("applicantDept", instance.getStartDeptName() == null ? "" : instance.getStartDeptName());
            values.put("startTime", instance.getStartTime() == null ? "" : instance.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            try {
                if (instance.getFormData() != null) {
                    String json = cn.hutool.json.JSONUtil.toJsonStr(instance.getFormData());
                    Map<String, Object> form = cn.hutool.json.JSONUtil.toBean(json, Map.class);
                    for (Map.Entry<String, Object> e : form.entrySet()) {
                        values.putIfAbsent(e.getKey(), e.getValue() == null ? "" : e.getValue());
                    }
                }
            } catch (Exception ignore) {
            }
        }
        if (dto.getPlaceholderValues() != null) {
            for (Map.Entry<String, Object> e : dto.getPlaceholderValues().entrySet()) {
                values.put(e.getKey(), e.getValue() == null ? "" : e.getValue());
            }
        }
        return values;
    }

    @Override
    public List<WfRedocGeneratedVO> listByInstance(String instanceNo) {
        if (StrUtil.isBlank(instanceNo)) return Collections.emptyList();
        LambdaQueryWrapper<WfRedocGenerated> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfRedocGenerated::getInstanceNo, instanceNo);
        wrapper.eq(WfRedocGenerated::getIsDeleted, 0);
        wrapper.orderByDesc(WfRedocGenerated::getCreateTime);
        return generatedMapper.selectList(wrapper).stream()
                .map(this::toGeneratedVO).collect(Collectors.toList());
    }

    @Override
    public IPage<WfRedocGeneratedVO> pageGenerated(long current, long size, String instanceNo, String templateId) {
        LambdaQueryWrapper<WfRedocGenerated> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(instanceNo)) wrapper.eq(WfRedocGenerated::getInstanceNo, instanceNo);
        if (StrUtil.isNotBlank(templateId)) wrapper.eq(WfRedocGenerated::getTemplateId, templateId);
        wrapper.eq(WfRedocGenerated::getIsDeleted, 0);
        wrapper.orderByDesc(WfRedocGenerated::getCreateTime);
        return generatedMapper.selectPage(new Page<>(current, size), wrapper).convert(this::toGeneratedVO);
    }

    @Override
    public WfRedocGeneratedVO getGeneratedDetail(Long id) {
        WfRedocGenerated g = generatedMapper.selectById(id);
        if (g == null) throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        return toGeneratedVO(g);
    }

    @Override
    public boolean markPrinted(Long id) {
        WfRedocGenerated g = generatedMapper.selectById(id);
        if (g == null) return false;
        g.setPrintCount((g.getPrintCount() == null ? 0 : g.getPrintCount()) + 1);
        g.setLastPrintTime(LocalDateTime.now());
        g.setLastPrintBy(SecurityUtils.getCurrentUserIdOrNull());
        return generatedMapper.updateById(g) > 0;
    }

    @Override
    public boolean markDownloaded(Long id) {
        WfRedocGenerated g = generatedMapper.selectById(id);
        if (g == null) return false;
        g.setDownloadCount((g.getDownloadCount() == null ? 0 : g.getDownloadCount()) + 1);
        g.setLastDownloadTime(LocalDateTime.now());
        g.setLastDownloadBy(SecurityUtils.getCurrentUserIdOrNull());
        return generatedMapper.updateById(g) > 0;
    }

    @Override
    public byte[] batchPrintPdf(WfRedocBatchDTO dto) {
        List<WfRedocGenerated> list = resolveBatch(dto);
        if (list.isEmpty()) return new byte[0];
        List<byte[]> pdfList = new ArrayList<>();
        for (WfRedocGenerated g : list) {
            if (g.getPdfFileId() != null) {
                byte[] bytes = storageHelper.getBytes(g.getPdfFileId());
                if (bytes != null && bytes.length > 0) pdfList.add(bytes);
            }
            markPrinted(g.getId());
        }
        try {
            return wordToPdfConverter.mergePdfs(pdfList);
        } catch (Exception e) {
            log.error("批量合并PDF失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "批量打印失败");
        }
    }

    @Override
    public byte[] batchDownload(WfRedocBatchDTO dto) {
        List<WfRedocGenerated> list = resolveBatch(dto);
        if (list.isEmpty()) return new byte[0];
        List<Long> fileIds = new ArrayList<>();
        for (WfRedocGenerated g : list) {
            if (g.getPdfFileId() != null) fileIds.add(g.getPdfFileId());
            if (g.getWordFileId() != null) fileIds.add(g.getWordFileId());
            markDownloaded(g.getId());
        }
        return storageHelper.batchZip(fileIds);
    }

    private List<WfRedocGenerated> resolveBatch(WfRedocBatchDTO dto) {
        if (dto == null) return Collections.emptyList();
        if (dto.getIds() != null && !dto.getIds().isEmpty()) {
            return generatedMapper.selectBatchIds(dto.getIds());
        }
        if (dto.getInstanceNos() != null && !dto.getInstanceNos().isEmpty()) {
            LambdaQueryWrapper<WfRedocGenerated> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(WfRedocGenerated::getInstanceNo, dto.getInstanceNos());
            wrapper.eq(WfRedocGenerated::getIsDeleted, 0);
            return generatedMapper.selectList(wrapper);
        }
        return Collections.emptyList();
    }

    private WfRedocTemplateVO toTemplateVO(WfRedocTemplate t) {
        WfRedocTemplateVO vo = new WfRedocTemplateVO();
        BeanUtils.copyProperties(t, vo);
        return vo;
    }

    private WfRedocGeneratedVO toGeneratedVO(WfRedocGenerated g) {
        WfAttachment wordAtt = storageHelper.getAttachment(g.getWordFileId());
        WfAttachment pdfAtt = storageHelper.getAttachment(g.getPdfFileId());
        return toGeneratedVO(g, wordAtt, pdfAtt);
    }

    private WfRedocGeneratedVO toGeneratedVO(WfRedocGenerated g, WfAttachment wordAtt, WfAttachment pdfAtt) {
        WfRedocGeneratedVO vo = new WfRedocGeneratedVO();
        BeanUtils.copyProperties(g, vo);
        vo.setWordPreviewUrl(wordAtt == null ? null : storageHelper.previewUrl(wordAtt));
        vo.setWordDownloadUrl(wordAtt == null ? null : storageHelper.previewUrl(wordAtt));
        vo.setPdfPreviewUrl(pdfAtt == null ? null : storageHelper.previewUrl(pdfAtt));
        vo.setPdfDownloadUrl(pdfAtt == null ? null : storageHelper.previewUrl(pdfAtt));
        return vo;
    }
}
