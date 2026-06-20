package com.innerworkflow.form.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word模板占位符替换工具
 * 基于Apache POI XWPF处理.docx
 * 支持段落、表格单元格、页眉页脚中的{placeholder}替换
 * 支持跨Run的占位符（即占位符被拆分成多个XWPFRun）
 */
@Slf4j
@Component
public class WordTemplateEngine {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)}");

    /**
     * 从模板字节数组解析出所有占位符（用于表单预览和校验）
     */
    public Set<String> extractPlaceholders(byte[] templateBytes) {
        Set<String> placeholders = new LinkedHashSet<>();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(templateBytes))) {
            collectFromParagraphs(doc.getParagraphs(), placeholders);
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        collectFromParagraphs(cell.getParagraphs(), placeholders);
                    }
                }
            }
            for (XWPFHeader header : doc.getHeaderList()) {
                collectFromParagraphs(header.getParagraphs(), placeholders);
            }
            for (XWPFFooter footer : doc.getFooterList()) {
                collectFromParagraphs(footer.getParagraphs(), placeholders);
            }
        } catch (Exception e) {
            log.error("解析Word模板占位符失败", e);
        }
        return placeholders;
    }

    private void collectFromParagraphs(List<XWPFParagraph> paragraphs, Set<String> placeholders) {
        for (XWPFParagraph p : paragraphs) {
            String fullText = p.getText();
            if (fullText == null || fullText.isEmpty()) continue;
            Matcher m = PLACEHOLDER_PATTERN.matcher(fullText);
            while (m.find()) {
                placeholders.add(m.group(1));
            }
        }
    }

    /**
     * 执行占位符替换，返回处理后的字节数组
     * @param templateBytes 原始模板字节
     * @param values key=占位符名（不含{}）, value=替换值
     */
    public byte[] process(byte[] templateBytes, Map<String, Object> values) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(templateBytes))) {
            Map<String, String> stringValues = convertToStringMap(values);

            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                replaceInParagraph(paragraph, stringValues);
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            replaceInParagraph(paragraph, stringValues);
                        }
                    }
                }
            }
            for (XWPFHeader header : doc.getHeaderList()) {
                for (XWPFParagraph paragraph : header.getParagraphs()) {
                    replaceInParagraph(paragraph, stringValues);
                }
            }
            for (XWPFFooter footer : doc.getFooterList()) {
                for (XWPFParagraph paragraph : footer.getParagraphs()) {
                    replaceInParagraph(paragraph, stringValues);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Word模板占位符替换失败", e);
            throw new RuntimeException("Word模板占位符替换失败: " + e.getMessage(), e);
        }
    }

    private Map<String, String> convertToStringMap(Map<String, Object> values) {
        Map<String, String> result = new HashMap<>();
        if (values == null) return result;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object v = entry.getValue();
            result.put(entry.getKey(), v == null ? "" : v.toString());
        }
        return result;
    }

    /**
     * 段落中跨Run的占位符替换
     * 先合并整段文本做正则匹配，然后按字符位置回溯到具体Run进行替换
     */
    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> values) {
        String fullText = paragraph.getText();
        if (fullText == null || fullText.isEmpty() || !fullText.contains("{")) {
            return;
        }

        List<int[]> runBoundaries = buildRunBoundaries(paragraph);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(fullText);
        List<int[]> replacements = new ArrayList<>();

        while (matcher.find()) {
            String placeholderKey = matcher.group(1);
            if (values.containsKey(placeholderKey)) {
                replacements.add(new int[]{matcher.start(), matcher.end(), values.get(placeholderKey).hashCode()});
            }
        }

        if (replacements.isEmpty()) return;

        for (int i = replacements.size() - 1; i >= 0; i--) {
            int[] rep = replacements[i];
            int start = rep[0];
            int end = rep[1];
            String replacement = null;
            Matcher m = PLACEHOLDER_PATTERN.matcher(fullText.substring(start, end));
            if (m.find()) {
                replacement = values.get(m.group(1));
            }
            if (replacement == null) replacement = "";
            applyReplacement(paragraph, runBoundaries, start, end, replacement);
        }
    }

    /**
     * 构建每个Run在整段文本中的字符范围[start,end)
     */
    private List<int[]> buildRunBoundaries(XWPFParagraph paragraph) {
        List<int[]> boundaries = new ArrayList<>();
        int pos = 0;
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text == null) text = "";
            int len = text.length();
            boundaries.add(new int[]{pos, pos + len});
            pos += len;
        }
        return boundaries;
    }

    private void applyReplacement(XWPFParagraph paragraph, List<int[]> boundaries,
                                  int start, int end, String replacement) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) return;

        int firstRunIdx = -1, lastRunIdx = -1;
        for (int i = 0; i < boundaries.size(); i++) {
            int[] b = boundaries.get(i);
            if (b[0] <= start && start < b[1]) firstRunIdx = i;
            if (b[0] < end && end <= b[1]) lastRunIdx = i;
        }
        if (firstRunIdx < 0) firstRunIdx = 0;
        if (lastRunIdx < 0) lastRunIdx = runs.size() - 1;

        XWPFRun firstRun = runs.get(firstRunIdx);
        String firstText = firstRun.getText(0);
        if (firstText == null) firstText = "";
        int prefixLen = Math.max(0, start - boundaries.get(firstRunIdx)[0]);
        String prefix = firstText.substring(0, prefixLen);

        XWPFRun lastRun = runs.get(lastRunIdx);
        String lastText = lastRun.getText(0);
        if (lastText == null) lastText = "";
        int suffixStart = Math.max(0, end - boundaries.get(lastRunIdx)[0]);
        String suffix = suffixStart >= lastText.length() ? "" : lastText.substring(suffixStart);

        firstRun.setText(prefix + replacement + suffix, 0);

        for (int i = lastRunIdx; i > firstRunIdx; i--) {
            paragraph.removeRun(i);
        }
    }
}
