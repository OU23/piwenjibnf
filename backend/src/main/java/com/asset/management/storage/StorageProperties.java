package com.asset.management.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储服务配置。
 *
 * <p>通过 {@code storage.type} 一键切换存储后端，默认 {@code local}：</p>
 *
 * <pre>
 * storage:
 *   type: local          # local | oss
 * </pre>
 *
 * <p>{@code type: oss} 时才会创建对象存储客户端，切回 local 无需改动任何业务代码。</p>
 */
@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** 存储类型：local（默认，本地磁盘）| oss（S3 兼容对象存储） */
    private StorageType type = StorageType.LOCAL;

    /** 存储渠道标识，会写入 sys_file；留空时按类型自动推导（local-default / oss-main） */
    private String channel = "";

    /** 本地磁盘存储配置 */
    private Local local = new Local();

    /** 对象存储配置 */
    private Oss oss = new Oss();

    @Data
    public static class Local {

        /** 存储根目录，支持相对路径（相对服务启动目录）与绝对路径 */
        private String basePath = "./data/upload";

        /** 文件访问前缀，用于拼出前端可访问的 URL */
        private String urlPrefix = "/api/file/preview";
    }

    @Data
    public static class Oss {

        /** 服务端点，如 http://127.0.0.1:9000（MinIO）、https://cos.ap-guangzhou.myqcloud.com */
        private String endpoint;

        /** 访问密钥 ID */
        private String accessKey;

        /** 访问密钥 Secret */
        private String secretKey;

        /** 存储桶名称 */
        private String bucket;

        /** 区域标识，绝大多数 S3 兼容服务填 us-east-1 即可 */
        private String region = "us-east-1";

        /** true=路径风格访问（MinIO 必须为 true），false=虚拟主机风格（AWS S3 / 腾讯云 COS） */
        private boolean pathStyle = true;

        /** 对象键统一前缀，便于多环境共用同一个桶 */
        private String keyPrefix = "";

        /**
         * 公网访问域名（含桶名或已绑定 CDN），配置后直接返回该直链；
         * 留空则返回带签名的临时访问地址。
         */
        private String publicDomain = "";

        /** 预签名访问地址有效期（秒），publicDomain 为空时生效 */
        private long presignedExpiry = 3600;

        /** 连接超时（毫秒） */
        private long connectTimeout = 10000;

        /** 读写超时（毫秒） */
        private long writeTimeout = 60000;
    }
}
