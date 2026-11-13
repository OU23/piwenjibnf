package com.asset.management.storage.config;

import com.asset.management.storage.StorageProperties;
import com.asset.management.storage.StorageService;
import com.asset.management.storage.impl.LocalStorageService;
import com.asset.management.storage.impl.OssStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储服务装配。
 *
 * <p>同一时刻只会注册一个 {@link StorageService} 实例，由 {@code storage.type} 决定：</p>
 * <ul>
 *   <li>{@code local}（缺省值）：本地磁盘存储，零外部依赖</li>
 *   <li>{@code oss}：S3 兼容对象存储</li>
 * </ul>
 *
 * <p>切换存储后端只改配置，业务代码无需改动。</p>
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
    public StorageService localStorageService(StorageProperties properties) {
        return new LocalStorageService(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "oss")
    public StorageService ossStorageService(StorageProperties properties) {
        return new OssStorageService(properties);
    }
}
