package com.asset.management.storage;

/**
 * 存储类型
 */
public enum StorageType {

    /** 本地磁盘存储（默认，零外部依赖） */
    LOCAL,

    /** S3 兼容对象存储（MinIO / 腾讯云 COS / AWS S3 / 七牛等） */
    OSS
}
