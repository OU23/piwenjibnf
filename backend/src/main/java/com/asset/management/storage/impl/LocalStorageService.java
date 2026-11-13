package com.asset.management.storage.impl;

import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.ResultCode;
import com.asset.management.storage.StorageContext;
import com.asset.management.storage.StorageKeyGenerator;
import com.asset.management.storage.StorageProperties;
import com.asset.management.storage.StorageService;
import com.asset.management.storage.StorageType;
import com.asset.management.storage.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

/**
 * 本地磁盘存储实现（默认）。
 *
 * <p>零外部依赖，开箱可用；对象键即相对路径，目录结构与对象存储保持一致，
 * 因此后续迁移到 OSS 时可以直接整目录同步上去。</p>
 */
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    /** 对象键的本地根目录 */
    private final Path root;

    /** 对外访问前缀 */
    private final String urlPrefix;

    private final String channel;

    public LocalStorageService(StorageProperties properties) {
        StorageProperties.Local config = properties.getLocal();
        this.root = Paths.get(config.getBasePath()).toAbsolutePath().normalize();
        String prefix = StringUtils.hasText(config.getUrlPrefix())
                ? config.getUrlPrefix().trim() : "/api/file/preview";
        this.urlPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        this.channel = StringUtils.hasText(properties.getChannel())
                ? properties.getChannel().trim() : "local-default";
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.STORAGE_ERROR, "本地存储目录创建失败：" + root);
        }
        log.info("文件存储使用【本地磁盘】模式，渠道：{}，根目录：{}，访问前缀：{}", channel, root, urlPrefix);
    }

    @Override
    public StoredFile upload(MultipartFile file, StorageContext context) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_EMPTY);
        }
        try (InputStream in = file.getInputStream()) {
            return upload(in, file.getOriginalFilename(), file.getSize(), file.getContentType(), context);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.STORAGE_ERROR, "读取上传文件失败：" + e.getMessage());
        }
    }

    @Override
    public StoredFile upload(InputStream in, String originalName, long size, String contentType,
                             StorageContext context) {
        String objectKey = StorageKeyGenerator.generate(
                context == null ? null : context.getBizType(),
                context == null ? null : context.getScope(),
                originalName);
        Path target = resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.STORAGE_ERROR, "本地文件写入失败：" + e.getMessage());
        }
        return buildStoredFile(objectKey, originalName, size, contentType);
    }

    @Override
    public InputStream download(String objectKey) {
        Path target = resolve(objectKey);
        if (!Files.exists(target)) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.STORAGE_ERROR, "本地文件读取失败：" + e.getMessage());
        }
    }

    @Override
    public boolean delete(String objectKey) {
        Path target = resolve(objectKey);
        try {
            return Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.STORAGE_ERROR, "本地文件删除失败：" + e.getMessage());
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.exists(resolve(objectKey));
    }

    @Override
    public String getUrl(String objectKey) {
        return urlPrefix + "/" + objectKey;
    }

    @Override
    public StorageType getType() {
        return StorageType.LOCAL;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    /**
     * 解析对象键为本地路径。
     *
     * <p>必须校验解析结果仍位于根目录内，防止 {@code ../} 穿越读到系统文件。</p>
     */
    private Path resolve(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ResultCode.STORAGE_ERROR, "非法的文件路径");
        }
        return target;
    }

    private StoredFile buildStoredFile(String objectKey, String originalName, long size, String contentType) {
        StoredFile stored = new StoredFile();
        stored.setObjectKey(objectKey);
        stored.setUrl(getUrl(objectKey));
        stored.setOriginalName(originalName);
        stored.setSize(size);
        stored.setContentType(contentType);
        stored.setStorageType(StorageType.LOCAL.name().toLowerCase());
        stored.setStorageChannel(channel);
        stored.setUploadTime(LocalDateTime.now());
        return stored;
    }
}
