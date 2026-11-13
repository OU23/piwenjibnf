package com.asset.management.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务单号 / 资产编号生成器
 * 格式：前缀 + yyyyMMddHHmmssSSS（毫秒） + 4 位原子序列号 + 4 位随机数
 *
 * <p>并发安全说明：
 * 时间戳精确到毫秒，配合 JVM 内 4 位原子序列号（同毫秒内 0~9999 轮转）
 * 与 4 位随机数双重兜底，同一毫秒内并发碰撞概率极低。
 * 数据库层对资产编号（asset_code）建有唯一索引作为最终兜底；
 * 申请单号、分配单号、归还单号建议同样建立唯一索引（见 schema.sql）。</p>
 */
public final class BizNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** JVM 内原子序列号，消除同毫秒并发下的重复可能 */
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private BizNoGenerator() {
    }

    public static String generate(String prefix) {
        String time = LocalDateTime.now().format(FORMATTER);
        // floorMod 保证结果恒为 0~9999，避免 int 溢出后产生负数
        int seq = Math.floorMod(SEQUENCE.getAndIncrement(), 10000);
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return prefix + time + String.format("%04d", seq) + String.format("%04d", random);
    }

    public static String generateApplicationNo() {
        return generate("AP");
    }

    public static String generateAllocationNo() {
        return generate("AL");
    }

    public static String generateReturnNo() {
        return generate("RT");
    }

    public static String generateAssetCode() {
        return generate("ZC");
    }
}
