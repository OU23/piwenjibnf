# 可选 OSS 存储服务 · 接入方案

> 适用项目：企业资产管理系统（Spring Boot 3.2.5 + MyBatis-Plus + MySQL 8 + Vue3）
> 设计参考：Network-Drive（risesoft-y9 / y9-storage，`https://github.com/risesoft-y9/Network-Drive`）

---

## 一、设计取舍：从 Network-Drive 借什么

Network-Drive 是个成熟的网络硬盘系统。它值得借鉴的不是具体代码，而是几条边界划分：

| 它的做法 | 我们怎么用 | 为什么 |
|---|---|---|
| 业务表只存 `fileStoreId`，真实路径不落库 | 业务表只存 `sys_file.object_key` | 换存储后端时业务数据零改动 |
| 访问地址由 `storageBaseUrl` 现拼（`/s/{fileStoreId}.{suffix}`） | `url` 不入库，查询时按当前配置动态生成 | 数据库里没有写死的域名和路径 |
| `listType`（my / shared / public / dept）+ `orgId` 组织归属 | `scope`（personal / dept / public）+ `dept_name` | 文件按组织维度归档、检索、审计 |
| 部门文件以「岗位」为主体（`orgUnitApi.getOrgUnitParent`） | 简化为按 `sys_user.department` 归属 | 本项目暂无岗位表，有的话可平滑升级 |
| 引用计数 + 软删除，物理删除与业务删除解耦 | `ref_count` + 逻辑删除 | 秒传复用、多业务引用同一文件时不会误删 |
| 存储实现是独立 jar（local / ftp），换 pom 即换后端 | Spring 条件装配，改一行配置即换后端 | 比换依赖更轻，且支持运行时切换 |
| 分片上传 + `Chunk` 表 + 断点续传 | 暂未实现，见第五节扩展阶梯 | 当前量级不需要 |

**刻意没有照搬的部分**：它的文件树（`parentId` 自引用）、分享外链、收藏、标签，属于"网盘"产品形态，资产系统不需要。我们只取存储抽象与元数据模型这两层。

---

## 二、接入方式

### 2.1 后端：业务方只有三步

```java
@Resource
private FileService fileService;

// 1) 上传：bizType 声明这是哪类工具的文件
FileUploadRequest req = new FileUploadRequest();
req.setBizType("tool");
req.setBizId(toolId);
req.setScope(StorageScope.DEPT);
SysFile file = fileService.upload(multipartFile, req);

// 2) 业务表里存 objectKey，不存 URL
tool.setSpecFileKey(file.getObjectKey());

// 3) 需要展示时现拼地址
String url = fileService.getUrl(tool.getSpecFileKey());
```

业务代码不知道文件落在本地还是 OSS，这就是接入的全部成本。

### 2.2 前端：调用方式与后端存储位置无关

```js
import { uploadFile, fileUrl } from '@/api'

const formData = new FormData()
formData.append('file', rawFile)
const { data } = await uploadFile(formData, { bizType: 'tool', bizId: toolId, scope: 'dept' })

// 展示：本地和 OSS 都是同一个调用
// <img :src="fileUrl(data.objectKey)" />
```

### 2.3 接口清单

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/api/file/upload` | 上传（bizType / bizId / scope） | 需登录 |
| POST | `/api/file/check` | 秒传校验（传 md5 + size） | 需登录 |
| GET | `/api/file/page` | 分页查询（按业务 / 部门 / 存储类型） | 需登录 |
| GET | `/api/file/biz` | 查询某业务对象名下的全部文件 | 需登录 |
| DELETE | `/api/file/{id}` | 删除（引用计数归零才删物理文件） | 需登录 |
| GET | `/api/file/preview/**` | 预览 / 下载（`?download=true`） | **放行** |
| GET | `/api/file/storage` | 当前存储类型与渠道 | 需登录 |
| GET | `/api/file/statistic` | 按存储后端统计（观察迁移进度） | 需登录 |

> **预览为什么放行**：`<img>`、`<video>` 无法携带请求头，加 JWT 校验图片就加载不出来。不可枚举性由 objectKey 中的 UUID 保证。若文件涉密，应改用「预签名地址 + 短有效期」，而不是继续放宽鉴权。

---

## 三、需要改动或新增的模块

### 3.1 本次已落地

| 模块 | 文件 | 职责 |
|---|---|---|
| 存储抽象 | `storage/StorageService.java` | 统一接口：upload / download / delete / exists / getUrl / getChannel |
| 存储配置 | `storage/StorageProperties.java` | 绑定 `storage.*`，含 local 与 oss 两组 |
| 存储上下文 | `storage/StorageContext.java` | bizType + scope，决定对象键分区 |
| 归属范围 | `storage/StorageScope.java` | personal / dept / public |
| 键生成 | `storage/StorageKeyGenerator.java` | `{bizType}/{scope}/{yyyyMM}/{uuid}.{ext}` |
| 本地实现 | `storage/impl/LocalStorageService.java` | 默认后端，含路径穿越防护 |
| OSS 实现 | `storage/impl/OssStorageService.java` | MinIO SDK 8.5.12，S3 兼容协议 |
| 条件装配 | `storage/config/StorageConfig.java` | 按 `storage.type` 装配唯一实现 |
| 元数据 | `entity/SysFile.java` + `mapper/SysFileMapper.java` | `sys_file` 表映射 |
| 门面服务 | `service/FileService.java` | 上传落库 / 秒传 / 引用计数删除 / 统计 |
| 接口层 | `controller/FileController.java` | 8 个 REST 接口 |
| 建表脚本 | `resources/sql/upgrade_file_storage.sql` | 增量 DDL，可重复执行 |

**改动的既有文件**：`pom.xml`（minio 依赖）、`application.yml`（storage 配置块）、`SecurityConfig`（放行预览路径）、`ResultCode`（1026–1029 错误码）、前端 `api/index.js`（文件接口封装）。

### 3.2 后续按需补充（当前未做）

| 模块 | 触发条件 | 说明 |
|---|---|---|
| 容量配额 | 需要限制个人 / 部门用量 | 仿 `StorageCapacity`（总量 + 余量），在 `FileService.upload` 里校验 |
| 分片上传 | 单文件经常超过 100MB | 需 `sys_file_chunk` 表 + 前端切片 |
| 回收站 | 需要支持误删恢复 | 现在直接逻辑删除，可加 `deleted_at` + 定时清理 |
| 文档在线预览 | 需要预览 office / pdf | Network-Drive 用 jodconverter，可独立部署转换服务 |
| 业务与文件关联表 | 一个对象挂多文件且要排序 / 说明 | 加 `biz_file_rel`（业务表 + file_id + sort + remark） |

---

## 四、与其他存储方式的切换逻辑

### 4.1 切换机制：条件装配 + 单一入口

```
storage.type = local  →  LocalStorageService   （默认，缺省即此）
storage.type = oss    →  OssStorageService
```

`StorageConfig` 用 `@ConditionalOnProperty` 保证同一时刻**只有一个实现**被注册，所有注入 `StorageService` 的地方不需要写任何 `if`。

### 4.2 切到 OSS：改配置即可，不动代码

```yaml
storage:
  type: oss
  channel: oss-main
  oss:
    endpoint: https://cos.ap-guangzhou.myqcloud.com
    access-key: 你的AK
    secret-key: 你的SK
    bucket: asset-management
    region: ap-guangzhou
    path-style: false                        # MinIO 用 true
    public-domain: https://cdn.example.com   # 留空则返回预签名地址
```

重启后，**新文件自动进 OSS，老文件仍在本地** —— 因为 `sys_file` 记录了每个文件的实际存储渠道。

### 4.3 渐进迁移（objectKey 与物理位置分离的价值）

```sql
-- 还剩多少文件在本地后端
SELECT storage_type, storage_channel, COUNT(*) AS cnt,
       ROUND(SUM(file_size) / 1024 / 1024, 2) AS mb
FROM sys_file WHERE deleted = 0
GROUP BY storage_type, storage_channel;
```

对应接口：`GET /api/file/statistic`。

迁移思路：按 `storage_channel` 捞出记录 → 逐个把文件从旧后端读出、写入新后端 → 更新该行的 `storage_type` / `storage_channel`。因为业务表存的是 objectKey 而不是 URL，**迁移过程中业务无需停机**，也不必改任何业务数据。

### 4.4 与各类存储方式的兼容性

| 目标 | 兼容性 | 说明 |
|---|---|---|
| MinIO | 直接可用 | S3 协议，`path-style: true` |
| 腾讯云 COS | 直接可用 | 官方支持 S3 兼容 API，`path-style: false` |
| AWS S3 | 直接可用 | — |
| 七牛 / 又拍等 S3 兼容服务 | 直接可用 | 填 endpoint 与域名即可 |
| 本地磁盘 | 默认后端 | 零外部依赖，开发与内网部署友好 |
| **阿里云 OSS** | **需新增实现** | 它不兼容 S3 协议，需引入 `aliyun-sdk-oss`，另写一个 `AliyunOssStorageService` 实现同一接口 |
| FTP / SFTP | 未实现 | 如需，同样按接口新增实现 |

**关键点**：新增任何后端，都只是"再写一个 `StorageService` 实现 + 加一个配置项"，业务代码、前端、数据库结构全都不用动。

---

## 五、工具数量增多后的存储与扩展方案

### 5.1 新增一类工具：零代码改动

扩展单位是 **bizType**，不是代码：

| 新增工具 | 传参 | 对象键落点 |
|---|---|---|
| 资产附件 | `bizType=asset` | `asset/{scope}/202609/...` |
| 巡检工具 | `bizType=inspection` | `inspection/{scope}/202609/...` |
| 耗材登记 | `bizType=consumable` | `consumable/{scope}/202609/...` |
| 合同文件 | `bizType=contract` | `contract/{scope}/202609/...` |

不建表、不改存储层、不加配置。检索全靠 `sys_file` 上的 `(biz_type, biz_id)` 索引。

### 5.2 对象键为什么分这三层

```
{bizType} / {scope} / {yyyyMM} / {uuid}.{ext}
    ↑          ↑          ↑
    │          │          └─ 避免单目录堆积；可按月设生命周期规则（如满一年转低频存储）
    │          └─ 个人 / 部门 / 公共物理隔离，便于整目录导出与审计
    └─ 某类工具的文件可整体迁移，或单独设桶与权限
```

部门名、上传人等业务维度**不进键**（中文编码麻烦，改名还会导致路径失效），只存数据库，靠索引检索。

### 5.3 量级增长时的应对阶梯

| 触发信号 | 应对措施 | 要改代码吗 |
|---|---|---|
| 文件名开始重复 | 无影响 | 否 |
| 单目录文件上万 | 已有年月分层，无需动作 | 否 |
| 某类工具文件特别多 | 给它单独配 `key-prefix` 或独立桶 | 否，仅改配置 |
| 单文件常超过 100MB | 上分片上传 + 断点续传 | 是（见 3.2） |
| 读多写少、要降成本 | 上传后转低频 / 归档存储，开 CDN 回源 | 否，改 OSS 侧策略 |
| 要按部门核算用量 | 加容量配额表 | 是（见 3.2） |
| 要清理临时文件 | 定时任务扫 `sys_file` 中无人引用的孤儿记录 | 是 |
| 从本地整体迁到 OSS | 按 `storage_channel` 分批迁移 | 是（见 4.3） |

### 5.4 多存储渠道并存

同一类型可以配多套后端（例如"普通文件进标准桶、涉密文件进私有桶"）。做法是把 OSS 配置改成 map 结构、按渠道选实现即可 —— `sys_file.storage_channel` 字段已经预留，届时**不需要改表**。

---

## 六、验证记录

在 18080 端口起临时实例实测（未影响 8080 上正在运行的实例）：

| 验证项 | 结果 |
|---|---|
| 上传并落库 | objectKey = `tool/dept/202609/7bdbb995...txt`，部门与上传人自动带出 |
| 秒传 | 重复上传返回同一 id 与 objectKey，`refCount` 1 → 2 |
| 按业务检索 | `bizType=tool` 精确命中 |
| 存储渠道 | 返回 `local / local-default` |
| 统计接口 | `fileCount=1, totalSize=32` |
| 匿名预览 | HTTP 200，Content-Type 正确，内容一致 |
| 引用计数删除 | 第一次删仅减计数（预览仍 200），第二次删掉物理文件后返回 1027 |
| 编译 | 110 个源文件 BUILD SUCCESS |

---

## 七、注意事项

1. **阿里云 OSS 不兼容 S3 协议**，是唯一需要额外写实现的云厂商。
2. **预览接口放行**是有意的取舍，靠 UUID 不可枚举保护。涉密文件应改用预签名地址 + 短有效期，或把预览改成鉴权接口、前端用 blob 加载。
3. 本地模式下的 `data/upload` 目录建议加入 `.gitignore`，并纳入备份范围。
4. 生产环境务必更换 `jwt.secret`；OSS 的 AK/SK 不要提交进版本库，建议走环境变量或配置中心。
5. `sys_file` 建表脚本使用 `CREATE TABLE IF NOT EXISTS`，在既有库上可重复执行。
