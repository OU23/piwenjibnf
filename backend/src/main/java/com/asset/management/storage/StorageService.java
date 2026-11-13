package com.asset.management.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储服务抽象。
 *
 * <p>业务代码只依赖本接口，不关心文件最终落在本地磁盘还是对象存储；
 * 切换后端只改配置，不动业务代码。</p>
 */
public interface StorageService {

    /**
     * 上传文件。
     *
     * @param file    上传的文件
     * @param context 存储上下文（业务分类 + 归属范围），决定对象键的分区
     * @return 存储结果（含 objectKey 与访问地址）
     */
    StoredFile upload(MultipartFile file, StorageContext context);

    /**
     * 上传文件（流式，便于从其他来源导入或做服务端中转）。
     *
     * @param in           输入流，由调用方负责关闭
     * @param originalName 原始文件名，用于推断扩展名
     * @param size         文件大小（字节），未知时传 -1
     * @param contentType  MIME 类型，未知时传 null
     * @param context      存储上下文
     */
    StoredFile upload(InputStream in, String originalName, long size, String contentType, StorageContext context);

    /**
     * 读取文件内容。
     *
     * @param objectKey 存储对象键
     */
    InputStream download(String objectKey);

    /**
     * 删除文件。
     *
     * @return 是否执行了删除操作
     */
    boolean delete(String objectKey);

    /** 判断文件是否存在 */
    boolean exists(String objectKey);

    /** 根据对象键拼出访问地址 */
    String getUrl(String objectKey);

    /** 当前生效的存储类型 */
    StorageType getType();

    /**
     * 当前存储渠道标识。
     *
     * <p>同一存储类型可以有多套后端（例如两个不同的 OSS 桶），
     * 渠道标识会写入 sys_file，作为后续按渠道迁移文件的依据。</p>
     */
    String getChannel();
}
