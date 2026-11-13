package com.asset.management.storage;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 对象键生成器。
 *
 * <p>键格式：{@code {bizType}/{scope}/{yyyyMM}/{uuid}.{ext}}，例如
 * {@code asset/dept/202609/1f3c9a2e4b7d4c8e9f0a1b2c3d4e5f60.jpg}。</p>
 *
 * <p>分三层的用意：</p>
 * <ul>
 *   <li><b>业务分类</b> —— 某一类工具的文件可以整体导出、迁移，或单独设置生命周期规则</li>
 *   <li><b>归属范围</b> —— 个人 / 部门 / 公共物理隔离存放，便于按组织维度归档与审计</li>
 *   <li><b>年月</b> —— 避免单目录文件堆积，也便于按时间做冷热分层</li>
 * </ul>
 *
 * <p>注意：部门名、上传人等业务检索维度<b>不进对象键</b>，只存数据库。
 * 中文进键会带来 URL 编码问题，且改部门名会导致路径失效。</p>
 */
public final class StorageKeyGenerator {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /** 默认业务分类 */
    public static final String DEFAULT_BIZ_TYPE = "common";

    /** 无法识别扩展名时的兜底后缀 */
    private static final String DEFAULT_EXTENSION = "bin";

    private StorageKeyGenerator() {
    }

    /**
     * 生成对象键。
     *
     * @param bizType      业务分类
     * @param scope        归属范围
     * @param originalName 原始文件名
     */
    public static String generate(String bizType, String scope, String originalName) {
        String biz = normalizeBizType(bizType);
        String scopeSegment = StorageScope.normalize(scope);
        String month = LocalDate.now().format(MONTH_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return biz + "/" + scopeSegment + "/" + month + "/" + uuid + "." + extractExtension(originalName);
    }

    /** 归一化业务分类，去掉路径分隔符等危险字符 */
    public static String normalizeBizType(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            return DEFAULT_BIZ_TYPE;
        }
        String cleaned = bizType.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return cleaned.isEmpty() ? DEFAULT_BIZ_TYPE : cleaned;
    }

    /** 提取小写扩展名，无法识别时返回 bin */
    public static String extractExtension(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return DEFAULT_EXTENSION;
        }
        String name = originalName.trim();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return DEFAULT_EXTENSION;
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return ext.isEmpty() ? DEFAULT_EXTENSION : ext;
    }
}
