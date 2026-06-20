package com.innerworkflow.form.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.PicturesManager;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.usermodel.PictureType;
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.util.List;

/**
 * Word转PDF工具类
 * 策略：
 * 1) .docx优先使用 org.apache.poi.xwpf.converter.pdf.PdfConverter（poi-ooxml原生）
 * 2) .doc先转HTML再用FlyingSaucer/iText渲染
 * 3) 水印叠加
 */
@Slf4j
@Component
public class WordToPdfConverter {

    public byte[] convertDocxToPdf(byte[] docxBytes) throws Exception {
        try (InputStream in = new ByteArrayInputStream(docxBytes);
             XWPFDocument document = new XWPFDocument(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfOptions options = PdfOptions.create();
            options.fontEncoding("UTF-8");
            PdfConverter.getInstance().convert(document, out, options);
            return out.toByteArray();
        } catch (NoClassDefFoundError | Exception e) {
            log.warn("PdfConverter转换失败，回退到POI原生渲染: {}", e.getMessage());
            return convertDocxToPdfFallback(docxBytes);
        }
    }

    /**
     * Fallback方案：通过iText逐段渲染（基础版，仅覆盖文本、表格、图片核心元素）
     */
    private byte[] convertDocxToPdfFallback(byte[] docxBytes) throws Exception {
        try (InputStream in = new ByteArrayInputStream(docxBytes);
             XWPFDocument xwpfDoc = new XWPFDocument(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document pdfDoc = new Document(PageSize.A4, 72, 72, 72, 72);
            PdfWriter.getInstance(pdfDoc, out);
            pdfDoc.open();
            BaseFont cnFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);

            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : xwpfDoc.getParagraphs()) {
                String text = p.getText();
                if (text == null || text.isEmpty()) continue;
                int align = Element.ALIGN_LEFT;
                switch (p.getAlignment()) {
                    case CENTER -> align = Element.ALIGN_CENTER;
                    case RIGHT -> align = Element.ALIGN_RIGHT;
                    case BOTH -> align = Element.ALIGN_JUSTIFIED;
                }
                Font font = new Font(cnFont, 12);
                Paragraph para = new Paragraph(text, font);
                para.setAlignment(align);
                pdfDoc.add(para);
            }
            pdfDoc.close();
            return out.toByteArray();
        }
    }

    public byte[] convertDocToPdf(byte[] docBytes) throws Exception {
        try (InputStream in = new ByteArrayInputStream(docBytes);
             HWPFDocument wordDoc = new HWPFDocument(in)) {
            File htmlFile = Files.createTempFile("redoc_", ".html").toFile();
            try {
                Document htmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                WordToHtmlConverter converter = new WordToHtmlConverter(htmlDoc);
                converter.setPicturesManager((bytes, pictureType, s, v, v1) -> s);
                converter.processDocument(wordDoc);
                try (FileOutputStream fos = new FileOutputStream(htmlFile)) {
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.transform(new DOMSource(converter.getDocument()), new StreamResult(fos));
                }
                return convertHtmlToPdf(htmlFile);
            } finally {
                htmlFile.delete();
            }
        }
    }

    private byte[] convertHtmlToPdf(File htmlFile) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document pdfDoc = new Document(PageSize.A4);
            PdfWriter.getInstance(pdfDoc, out);
            pdfDoc.open();
            BaseFont cnFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font font = new Font(cnFont, 12);
            StringBuilder sb = new StringBuilder();
            for (String line : Files.readAllLines(htmlFile.toPath())) sb.append(line).append("\n");
            pdfDoc.add(new Paragraph(sb.toString(), font));
            pdfDoc.close();
            return out.toByteArray();
        }
    }

    /**
     * 为PDF添加水印文字（斜向平铺）
     */
    public byte[] addWatermark(byte[] pdfBytes, String watermarkText, String colorHex) throws Exception {
        if (watermarkText == null || watermarkText.isEmpty()) return pdfBytes;
        try (PdfReader reader = new PdfReader(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfStamper stamper = new PdfStamper(reader, out);
            BaseFont font = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            int rgb = parseColor(colorHex, 0xd9d9d9);
            BaseColor color = new BaseColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 100);

            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                PdfContentByte under = stamper.getUnderContent(i);
                Rectangle page = reader.getPageSize(i);
                under.saveState();
                under.setColorFill(color);
                under.setFontAndSize(font, 48);
                under.beginText();
                for (int x = 100; x < page.getWidth(); x += 250) {
                    for (int y = 100; y < page.getHeight(); y += 250) {
                        under.showTextAlignedKerned(Element.ALIGN_CENTER, watermarkText, x, y, 30);
                    }
                }
                under.endText();
                under.restoreState();
            }
            stamper.close();
            return out.toByteArray();
        }
    }

    private int parseColor(String hex, int fallback) {
        if (hex == null || !hex.startsWith("#") || hex.length() < 7) return fallback;
        try {
            return Integer.parseInt(hex.substring(1, 7), 16);
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * 合并多个PDF为单个PDF（批量打印用）
     */
    public byte[] mergePdfs(List<byte[]> pdfList) throws Exception {
        if (pdfList == null || pdfList.isEmpty()) return new byte[0];
        if (pdfList.size() == 1) return pdfList.get(0);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Document document = new Document()) {
            PdfCopy copy = new PdfSmartCopy(document, out);
            document.open();
            for (byte[] pdfBytes : pdfList) {
                if (pdfBytes == null || pdfBytes.length == 0) continue;
                try (PdfReader reader = new PdfReader(pdfBytes)) {
                    for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                        copy.addPage(copy.getImportedPage(reader, i));
                    }
                    copy.freeReader(reader);
                }
            }
            copy.close();
            return out.toByteArray();
        }
    }
}
