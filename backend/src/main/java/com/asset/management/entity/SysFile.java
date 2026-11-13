package com.asset.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件元数据。
 *
 * <p>设计借鉴 Network-Drive（y9-storage）的做法：<b>业务表只保存 objectKey，不保存真实存储路径</b>，
 * 由存储服务根据 objectKey 定位文件。这样切换存储后端（本地 → OSS）时业务数据无需改动。</p>
 *
 * <p>其中 {@code storageType} / {@code storageChannel} 记录文件<b>实际</b>落在哪个后端，
 * 因此支持新旧文件分散在不同后端（渐进迁移），而不是全库一刀切。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 存储对象键：业务表保存这个值，而不是完整 URL */
    private String objectKey;

    /** 上传时的原始文件名 */
    private String originalName;

    /** 扩展名（小写，不含点） */
    private String fileSuffix;

    /** MIME 类型 */
    private String contentType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件内容 MD5，用于秒传与去重 */
    private String md5;

    /** 实际落库的存储类型：local / oss */
    private String storageType;

    /** 存储渠道标识（如 local-default、oss-main），多后端并存与迁移的依据 */
    private String storageChannel;

    /** 业务分类：每新增一类工具声明一个 bizType 即可，如 asset / tool / contract */
    private String bizType;

    /** 业务主键，如资产 ID、工具 ID */
    private Long bizId;

    /** 归属范围：personal 个人 / dept 部门 / public 公共 */
    private String scope;

    /** 归属部门名称（对应 sys_user.department） */
    private String deptName;

    /** 上传人 ID */
    private Long uploaderId;

    /** 上传人姓名 */
    private String uploaderName;

    /** 引用计数：秒传复用或多个业务引用同一文件时大于 1，归零才删物理文件 */
    private Integer refCount;

    /**
     * 访问地址：不入库，查询后按当前存储配置动态生成。
     *
     * <p>这是切换存储后端时业务无感的另一个关键 —— 数据库里没有 URL，
     * 存储方式变了，重新生成的地址自然跟着变。</p>
     */
    @TableField(exist = false)
    private String url;
}
