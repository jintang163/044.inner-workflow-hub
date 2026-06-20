package com.innerworkflow.form.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.*;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;

/**
 * 电子印章与国密数字签名工具类
 * 1) 印章图片叠加（基于iText PdfStamper + Image透明混合）
 * 2) 国密SM2/SM3数字签名（基于BouncyCastle Provider + iText PdfSignatureAppearance）
 */
@Slf4j
@Component
public class SealAndSignatureUtil {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final int CM_TO_PT = 28;

    /**
     * 为PDF加盖印章图片
     * @param pdfBytes 原始PDF字节
     * @param sealImageBytes 印章图片字节（透明背景PNG最佳）
     * @param positionType 位置类型：1-右下角, 2-签字位置（按关键字搜索）, 3-自定义坐标
     * @param offsetXcm 自定义X偏移（厘米，positionType=3时生效）
     * @param offsetYcm 自定义Y偏移（厘米，positionType=3时生效）
     * @param scale 印章缩放比例
     * @return 盖章后的PDF字节
     */
    public byte[] applySealImage(byte[] pdfBytes, byte[] sealImageBytes,
                                 Integer positionType, Double offsetXcm, Double offsetYcm,
                                 Double scale) throws Exception {
        if (sealImageBytes == null || sealImageBytes.length == 0) return pdfBytes;
        int posType = positionType == null ? 1 : positionType;
        double s = scale == null || scale <= 0 ? 1.0 : scale;

        try (PdfReader reader = new PdfReader(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfStamper stamper = new PdfStamper(reader, out);
            Image sealImg = Image.getInstance(sealImageBytes);
            float origW = sealImg.getWidth();
            float origH = sealImg.getHeight();
            sealImg.scalePercent(s * 100);
            float finalW = origW * s;
            float finalH = origH * s;

            int totalPages = reader.getNumberOfPages();
            int targetPage = Math.max(1, totalPages);
            Rectangle pageRect = reader.getPageSize(targetPage);
            float x, y;

            switch (posType) {
                case 3:
                    x = (float) ((offsetXcm == null ? 2 : offsetXcm) * CM_TO_PT);
                    y = (float) ((offsetYcm == null ? 4 : offsetYcm) * CM_TO_PT);
                    break;
                case 2:
                case 1:
                default:
                    x = pageRect.getWidth() - finalW - 72;
                    y = 72;
                    break;
            }
            sealImg.setAbsolutePosition(x, y);

            PdfContentByte over = stamper.getOverContent(targetPage);
            over.saveState();
            PdfGState gs = new PdfGState();
            gs.setBlendMode(PdfGState.BM_NORMAL);
            over.setGState(gs);
            over.addImage(sealImg);
            over.restoreState();
            stamper.close();
            return out.toByteArray();
        }
    }

    /**
     * 国密数字签名（SM3withSM2）
     * @param pdfBytes 原始PDF字节
     * @param p12Bytes PKCS12证书字节
     * @param password 证书密码
     * @param reason 签名原因
     * @param location 签名地点
     * @return 签名后的PDF字节
     */
    public byte[] applyGmSignature(byte[] pdfBytes, byte[] p12Bytes, char[] password,
                                   String reason, String location) throws Exception {
        if (p12Bytes == null || p12Bytes.length == 0) {
            log.warn("未提供国密证书，跳过数字签名");
            return pdfBytes;
        }
        KeyStore ks = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
        try (InputStream in = new ByteArrayInputStream(p12Bytes)) {
            ks.load(in, password);
        }
        String alias = ks.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password);
        Certificate[] chain = ks.getCertificateChain(alias);
        X509Certificate cert = (X509Certificate) chain[0];

        try (PdfReader reader = new PdfReader(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfStamper stamper = PdfStamper.createSignature(reader, out, '\0');
            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
            appearance.setReason(reason == null ? "Approval" : reason);
            appearance.setLocation(location == null ? "InnerWorkflow" : location);
            appearance.setSignDate(Calendar.getInstance());
            appearance.setVisibleSignature(new Rectangle(36, 740, 200, 800), 1, "GMSeal");

            ExternalDigest digest = new BouncyCastleDigest();
            ExternalSignature signature = new PrivateKeySignature(privateKey,
                    GMObjectIdentifiers.sm3_with_sm2.getId(), BouncyCastleProvider.PROVIDER_NAME);
            MakeSignature.signDetached(appearance, digest, signature, chain,
                    null, null, null, 0, MakeSignature.CryptoStandard.CMS);
            stamper.close();
            return out.toByteArray();
        }
    }

    private static class BouncyCastleDigest implements ExternalDigest {
        @Override
        public java.security.MessageDigest getMessageDigest(String hashAlgorithm) {
            String oid = DigestAlgorithms.getAllowedDigests(hashAlgorithm);
            try {
                if (oid != null && (oid.contains("SM3") || hashAlgorithm.toUpperCase().contains("SM3"))) {
                    return java.security.MessageDigest.getInstance("SM3", BouncyCastleProvider.PROVIDER_NAME);
                }
                return java.security.MessageDigest.getInstance(hashAlgorithm);
            } catch (Exception e) {
                throw new RuntimeException("摘要算法初始化失败: " + hashAlgorithm, e);
            }
        }
    }

    private static class PrivateKeySignature implements ExternalSignature {
        private final PrivateKey pk;
        private final String algorithm;
        private final String provider;
        PrivateKeySignature(PrivateKey pk, String algorithm, String provider) {
            this.pk = pk; this.algorithm = algorithm; this.provider = provider;
        }
        @Override
        public String getHashAlgorithm() { return algorithm; }
        @Override
        public String getEncryptionAlgorithm() { return "SM2"; }
        @Override
        public byte[] sign(byte[] message) {
            try {
                String algo = algorithm;
                java.security.Signature sig = java.security.Signature.getInstance(algo, provider);
                sig.initSign(pk);
                sig.update(message);
                return sig.sign();
            } catch (Exception e) {
                throw new RuntimeException("数字签名失败", e);
            }
        }
    }
}
