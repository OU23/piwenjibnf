package com.asset.management.storage;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件存储结果。
 *
 * <p>{@code url} 由具体存储实现生成，前端无需感知文件到底存在本地还是对象存储。</p>
 */
@Data
public class StoredFile implements Serializable {

    /** 存储对象键（业务表里保存这个值，而不是完整 URL） */
    private String objectKey;

    /** 可直接访问的地址 */
    private String url;

    /** 上传时的原始文件名 */
    private String originalName;

    /** 文件大小（字节） */
    private long size;

    /** MIME 类型 */
    private String contentType;

    /** 实际落库的存储类型：local / oss */
    private String storageType;

    /** 存储渠道标识，多后端并存与迁移的依据 */
    private String storageChannel;

    /** 上传时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;
}
