package com.asset.management.storage.impl;

import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.ResultCode;
import com.asset.management.storage.StorageContext;
import com.asset.management.storage.StorageKeyGenerator;
import com.asset.management.storage.StorageProperties;
import com.asset.management.storage.StorageService;
import com.asset.management.storage.StorageType;
import com.asset.management.storage.StoredFile;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * S3 兼容对象存储实现（MinIO / 腾讯云 COS / AWS S3 / 七牛等）。
 *
 * <p>仅在 {@code storage.type=oss} 时装配。两个刻意的设计：</p>
 * <ul>
 *   <li><b>客户端懒连接</b> —— 构造时不访问对象存储，对象存储故障不会阻塞应用启动</li>
 *   <li><b>惰性建桶</b> —— 首次真正上传时才检查并创建存储桶</li>
 * </ul>
 */
public class OssStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(OssStorageService.class);

    /** 流式上传（长度未知）时的分片大小：10MB */
    private static final long PART_SIZE = 10L * 1024 * 1024;

    /** 兜底访问地址前缀：对象存储不可直连时走后端代理 */
    private static final String PROXY_URL_PREFIX = "/api/file/preview";

    private final StorageProperties.Oss config;
    private final MinioClient client;
    private final String channel;

    /** 桶是否已就绪，首次上传时惰性初始化 */
    private volatile boolean bucketReady = false;

    public OssStorageService(StorageProperties properties) {
        this.config = properties.getOss();
        this.channel = StringUtils.hasText(properties.getChannel())
                ? properties.getChannel().trim() : "oss-main";
        validate();

        this.client = MinioClient.builder()
                .endpoint(config.getEndpoint())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
        this.client.setTimeout(config.getConnectTimeout(), config.getWriteTimeout(), config.getWriteTimeout());

        log.info("文件存储使用【对象存储】模式，渠道：{}，endpoint：{}，bucket：{}，pathStyle：{}",
                channel, config.getEndpoint(), config.getBucket(), config.isPathStyle());
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
        ensureBucketReady();

        String objectKey = buildObjectKey(
                context == null ? null : context.getBizType(),
                context == null ? null : context.getScope(),
                originalName);
        try {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(config.getBucket())
                    .object(objectKey);
            if (size >= 0) {
                builder.stream(in, size, -1);
            } else {
                // 长度未知时必须显式指定分片大小
                builder.stream(in, -1, PART_SIZE);
            }
            if (StringUtils.hasText(contentType)) {
                builder.contentType(contentType);
            }
            client.putObject(builder.build());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.STORAGE_ERROR, "对象存储上传失败：" + rootMessage(e));
        }
        return buildStoredFile(objectKey, originalName, size, contentType);
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(config.getBucket())
                    .object(objectKey)
                    .build());
        } catch (ErrorResponseException e) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.STORAGE_ERROR, "对象存储读取失败：" + rootMessage(e));
        }
    }

    @Override
    public boolean delete(String objectKey) {
        if (!exists(objectKey)) {
            return false;
        }
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(config.getBucket())
                    .object(objectKey)
                    .build());
            return true;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.STORAGE_ERROR, "对象存储删除失败：" + rootMessage(e));
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            client.statObject(StatObjectArgs.builder()
                    .bucket(config.getBucket())
                    .object(objectKey)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.STORAGE_ERROR, "对象存储访问失败：" + rootMessage(e));
        }
    }

    @Override
    public String getUrl(String objectKey) {
        // 1) 配了公网域名：直接返回稳定直链
        if (StringUtils.hasText(config.getPublicDomain())) {
            String domain = config.getPublicDomain().trim();
            if (domain.endsWith("/")) {
                domain = domain.substring(0, domain.length() - 1);
            }
            return domain + "/" + objectKey;
        }
        // 2) 否则返回带签名的临时地址
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(config.getBucket())
                    .object(objectKey)
                    .expiry((int) config.getPresignedExpiry(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.warn("生成预签名地址失败，回退为后端代理地址：{}", rootMessage(e));
            return PROXY_URL_PREFIX + "/" + objectKey;
        }
    }

    @Override
    public StorageType getType() {
        return StorageType.OSS;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    /** 拼接对象键，统一带上配置的前缀 */
    private String buildObjectKey(String bizType, String scope, String originalName) {
        String key = StorageKeyGenerator.generate(bizType, scope, originalName);
        String prefix = config.getKeyPrefix();
        if (!StringUtils.hasText(prefix)) {
            return key;
        }
        String normalized = prefix.trim().replaceAll("^/+", "").replaceAll("/+$", "");
        return normalized.isEmpty() ? key : normalized + "/" + key;
    }

    /** 惰性检查并创建存储桶 */
    private void ensureBucketReady() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            try {
                boolean exists = client.bucketExists(BucketExistsArgs.builder()
                        .bucket(config.getBucket())
                        .build());
                if (!exists) {
                    client.makeBucket(MakeBucketArgs.builder()
                            .bucket(config.getBucket())
                            .build());
                    log.info("对象存储桶 {} 不存在，已自动创建", config.getBucket());
                }
                bucketReady = true;
            } catch (Exception e) {
                throw new BusinessException(ResultCode.STORAGE_NOT_CONFIGURED,
                        "对象存储连接失败，请检查 storage.oss 配置：" + rootMessage(e));
            }
        }
    }

    private StoredFile buildStoredFile(String objectKey, String originalName, long size, String contentType) {
        StoredFile stored = new StoredFile();
        stored.setObjectKey(objectKey);
        stored.setUrl(getUrl(objectKey));
        stored.setOriginalName(originalName);
        stored.setSize(size);
        stored.setContentType(contentType);
        stored.setStorageType(StorageType.OSS.name().toLowerCase());
        stored.setStorageChannel(channel);
        stored.setUploadTime(LocalDateTime.now());
        return stored;
    }

    private void validate() {
        if (!StringUtils.hasText(config.getEndpoint())) {
            throw new BusinessException(ResultCode.STORAGE_NOT_CONFIGURED, "缺少配置项 storage.oss.endpoint");
        }
        if (!StringUtils.hasText(config.getAccessKey()) || !StringUtils.hasText(config.getSecretKey())) {
            throw new BusinessException(ResultCode.STORAGE_NOT_CONFIGURED,
                    "缺少配置项 storage.oss.access-key / secret-key");
        }
        if (!StringUtils.hasText(config.getBucket())) {
            throw new BusinessException(ResultCode.STORAGE_NOT_CONFIGURED, "缺少配置项 storage.oss.bucket");
        }
    }

    /** 取异常链最内层的可读信息，避免日志里只有包装异常 */
    private String rootMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
