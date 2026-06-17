package com.innerworkflow.common.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成工具类
 * <p>
 * 基于雪花算法生成分布式唯一ID，同时提供各类业务单号生成能力
 * </p>
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Slf4j
public class IdGenerator {

    /**
     * 日期格式
     */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 序列号（用于相同毫秒内的递增）
     */
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    /**
     * 生成雪花算法ID（19位数字，分布式唯一）
     * <p>
     * 使用Hutool的Snowflake算法，默认workerId和dataCenterId基于机器IP计算
     * </p>
     */
    public static Long nextId() {
        return IdUtil.getSnowflakeNextId();
    }

    /**
     * 生成雪花算法ID（字符串形式）
     */
    public static String nextIdStr() {
        return IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * 生成审批单号
     * <p>
     * 格式：AP + yyyyMMdd + 8位序列号，如：AP2024010100000001
     * </p>
     */
    public static String generateApprovalNo() {
        return generateBusinessNo("AP", 8);
    }

    /**
     * 生成任务号
     * <p>
     * 格式：TK + yyyyMMdd + 8位序列号，如：TK2024010100000001
     * </p>
     */
    public static String generateTaskNo() {
        return generateBusinessNo("TK", 8);
    }

    /**
     * 生成流程定义编号
     * <p>
     * 格式：PD + yyyyMMdd + 6位序列号，如：PD20240101000001
     * </p>
     */
    public static String generateProcessDefNo() {
        return generateBusinessNo("PD", 6);
    }

    /**
     * 生成表单编号
     * <p>
     * 格式：FM + yyyyMMdd + 6位序列号，如：FM20240101000001
     * </p>
     */
    public static String generateFormNo() {
        return generateBusinessNo("FM", 6);
    }

    /**
     * 通用业务单号生成
     *
     * @param prefix       前缀（2-3位）
     * @param sequenceLen  序列号位数
     */
    public static String generateBusinessNo(String prefix, int sequenceLen) {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        long seq = SEQUENCE.incrementAndGet() % (long) Math.pow(10, sequenceLen);
        return prefix + datePart + String.format("%0" + sequenceLen + "d", seq);
    }

    /**
     * 生成带时间戳的精确业务号
     * <p>
     * 格式：前缀 + yyyyMMddHHmmss + 4位随机数，如：AP20240101123045A1B2
     * </p>
     */
    public static String generatePreciseNo(String prefix) {
        String datetimePart = LocalDateTime.now().format(DATETIME_FORMAT);
        String randomPart = RandomUtil.randomStringUpper(4);
        return prefix + datetimePart + randomPart;
    }

    /**
     * 生成短ID（12位，雪花ID取中间部分）
     * <p>
     * 适用于对ID长度敏感且全局唯一性要求不极端的场景
     * </p>
     */
    public static String nextShortId() {
        String fullId = nextIdStr();
        // 取中间12位（去掉前3位和后4位，保留时间相关的高位）
        return fullId.length() >= 12 ? fullId.substring(3, 15) : fullId;
    }

    /**
     * 生成UUID（无横线，32位）
     */
    public static String nextUUID() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 生成带前缀的UUID
     */
    public static String nextUUID(String prefix) {
        return prefix + IdUtil.fastSimpleUUID();
    }
}
