package com.asset.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 企业资产管理系统启动入口
 *
 * @author Asset Management Team
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class AssetManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetManagementApplication.class, args);
        System.out.println("""
                ==================================================
                  企业资产管理系统启动成功
                  Swagger 文档: http://localhost:8080/swagger-ui.html
                ==================================================
                """);
    }
}
