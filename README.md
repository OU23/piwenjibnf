# 企业资产管理系统（Enterprise Asset Management System）

一个基于 **Spring Boot 3 + MyBatis-Plus + MySQL 8** 与 **Vue 3 + Element Plus + ECharts** 的完整可运行企业资产管理系统，覆盖资产从**分类 → 登记 → 申请 → 审批 → 分配 → 领用 → 归还 → 统计**的全生命周期闭环。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 用户认证与授权 | JWT 无状态登录、Spring Security + RBAC 权限校验、密码加密、菜单动态加载 |
| 用户管理 | 用户增删改查、角色分配、重置密码、修改密码 |
| 角色管理 | 角色增删改查、权限（菜单/按钮）分配 |
| 资产分类 | 多级分类树、分类属性配置 |
| 资产管理 | 资产登记、编辑、删除、报修、维修完成、报废，全生命周期状态机 |
| 资产申请 | 按分类/指定资产申请、审批通过/驳回、撤销 |
| 资产分配 | 手动分配、自动分配（乐观锁并发安全）、领用确认 |
| 资产归还 | 归还登记、归还审核（通过/驳回） |
| 统计分析 | 首页概览看板、状态分布、分类分布、趋势分析、可视化图表 |
| 操作日志 | AOP 切面自动记录所有资产相关操作的审计日志 |

## 技术栈

- **后端**：Java 17、Spring Boot 3.2.5、Spring Security 6、MyBatis-Plus 3.5.5、JWT（jjwt 0.11.5）、SpringDoc OpenAPI、Lombok
- **前端**：Vue 3.4、Vue Router 4、Pinia、Element Plus、Axios、ECharts、Vite 5
- **数据库**：MySQL 8（utf8mb4）

## 核心设计要点

- **状态机**：资产状态 `IN_STOCK → APPLIED → ALLOCATED → IN_USE → PENDING_RETURN → IN_STOCK`，以及 `REPAIRING`、`SCRAPPED` 分支，所有流转通过 `AssetService.transitionStatus` 校验合法来源状态集合。
- **并发与数据一致性**：资产表 `version` 字段 + `@Version` 乐观锁；自动分配在并发冲突时自动跳过冲突资产。
- **事务（ACID）**：申请、审批、分配、归还等业务均在 `@Transactional` 中保证原子性；手动分配整体事务「全成功或全回滚」。
- **RBAC 权限**：后端所有接口通过 `@PreAuthorize("hasAuthority('...')")` 严格校验，超级管理员（`admin`）自动拥有全部权限。
- **操作审计**：`@OperationLog` 注解 + AOP 切面，`@Async` 异步落库，记录操作人、模块、类型、请求地址、IP、耗时、结果。
- **全局异常处理**：`@RestControllerAdvice` 统一捕获业务异常、参数校验异常、权限异常，返回规范 `Result` 结构。

## 目录结构

```
asset-management-system/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/asset/management/
│       │   ├── common/               # 结果封装、异常、常量、注解
│       │   ├── config/               # 安全、MyBatis-Plus、Swagger、数据初始化
│       │   ├── controller/           # REST 接口
│       │   ├── dto/                  # 请求/响应对象
│       │   ├── entity/               # 数据库实体
│       │   ├── mapper/               # MyBatis-Plus Mapper
│       │   ├── security/             # JWT、登录用户、过滤器
│       │   ├── service/              # 业务逻辑
│       │   ├── aspect/               # 操作日志切面
│       │   └── util/                 # 工具类
│       └── resources/
│           ├── application.yml
│           └── sql/schema.sql, data.sql
└── frontend/                         # Vue 3 前端
    ├── package.json
    └── src/
        ├── api/                      # Axios 封装与接口
        ├── components/               # 侧边栏递归组件
        ├── layout/                   # 主布局
        ├── router/                   # 路由
        ├── store/                    # Pinia 状态
        └── views/                    # 各功能页面
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Node.js 18+（含 npm）

### 1. 初始化数据库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS asset_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -u root -p asset_management < backend/src/main/resources/sql/schema.sql
mysql -u root -p asset_management < backend/src/main/resources/sql/data.sql
```

### 2. 启动后端

修改 `backend/src/main/resources/application.yml` 中的数据库账号密码（默认 `root/root`），然后：

```bash
cd backend
mvn spring-boot:run
```

后端启动后监听 `http://localhost:8080`，Swagger 文档见 `http://localhost:8080/swagger-ui.html`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器监听 `http://localhost:5173`，已配置 `/api` 代理到后端 `8080`。

### 4. 登录

| 账号 | 密码 | 角色 | 权限 |
| --- | --- | --- | --- |
| admin | admin123 | 超级管理员 | 全部功能 |
| manager | 123456 | 资产管理员 | 资产、业务、统计 |
| employee | 123456 | 普通员工 | 查看资产、申请、领用、归还 |

> 三个演示账号由后端 `DataInitializer` 在首次启动时幂等创建（依赖 `data.sql` 已初始化角色）。

## 主要接口

- 认证：`POST /api/auth/login`、`GET /api/auth/info`、`PUT /api/auth/password`
- 用户：`/api/user/**`
- 角色：`/api/role/**`（含 `PUT /api/role/{id}/menus` 分配权限）
- 菜单：`GET /api/menu/tree`
- 分类：`/api/category/**`
- 资产：`/api/asset/**`（含 `scrap`/`repair`/`repairDone`）
- 申请：`/api/application/**`（提交、审批、撤销、流程）
- 分配：`/api/allocation/**`（手动、自动、领用确认）
- 归还：`/api/return/**`（提交、审核、流程）
- 统计：`/api/statistics/**`（dashboard、status、category、trend）
- 日志：`GET /api/log/page`

## 生产部署

### 后端打包

```bash
cd backend
mvn clean package -DskipTests
# 产物 backend/target/asset-management-*.jar
java -jar target/asset-management-*.jar
```

### 前端打包

```bash
cd frontend
npm run build
# 产物 frontend/dist
```

### Nginx 部署建议

将 `frontend/dist` 作为静态资源部署，并将 `/api` 反向代理到后端：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;   # 支持前端路由 history 模式
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 上线前检查清单

- 修改 `application.yml` 中的数据库账号密码与 `jwt.secret`（生产环境务必使用随机生成的 ≥64 字节密钥）。
- 关闭 MyBatis SQL 日志（`mybatis-plus.configuration.log-impl`）。
- 修改/删除演示账号，或调整 `DataInitializer`。
- 建议为后端配置 HTTPS 与独立域名。
