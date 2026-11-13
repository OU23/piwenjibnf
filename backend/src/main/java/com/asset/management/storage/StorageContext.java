package com.asset.management.storage;

import lombok.Data;

/**
 * 存储上下文：决定文件落在哪个业务分区、哪个归属范围。
 *
 * <p>新增一类工具时，只需在调用处传一个新的 {@code bizType}，
 * 存储层代码完全不用动。</p>
 */
@Data
public class StorageContext {

    /** 业务分类，如 asset / tool / contract */
    private String bizType = StorageKeyGenerator.DEFAULT_BIZ_TYPE;

    /** 归属范围：personal / dept / public */
    private String scope = StorageScope.PUBLIC;

    public StorageContext() {
    }

    public StorageContext(String bizType, String scope) {
        this.bizType = bizType;
        this.scope = scope;
    }

    public static StorageContext of(String bizType, String scope) {
        return new StorageContext(bizType, scope);
    }
}
