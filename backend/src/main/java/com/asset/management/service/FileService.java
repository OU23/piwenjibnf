package com.asset.management.service;

import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.ResultCode;
import com.asset.management.dto.FileQuery;
import com.asset.management.dto.FileUploadRequest;
import com.asset.management.entity.SysFile;
import com.asset.management.entity.SysUser;
import com.asset.management.mapper.SysFileMapper;
import com.asset.management.security.LoginUser;
import com.asset.management.security.SecurityUtil;
import com.asset.management.storage.StorageContext;
import com.asset.management.storage.StorageKeyGenerator;
import com.asset.management.storage.StorageScope;
import com.asset.management.storage.StorageService;
import com.asset.management.storage.StoredFile;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 文件服务门面：把「存储」和「元数据」两件事收口在一起。
 *
 * <p>业务方只需要 {@code fileService.upload(...)}，拿到 SysFile 里的 objectKey 存进自己的业务表即可，
 * 完全不用关心文件落在本地还是 OSS。</p>
 *
 * <p>借鉴 Network-Drive 的三点：</p>
 * <ol>
 *   <li>业务表只存 fileStoreId（对应这里的 objectKey），不存真实路径</li>
 *   <li>按归属范围（个人 / 部门 / 公共）组织文件，再按部门和业务检索</li>
 *   <li>物理文件删除与业务删除解耦，用引用计数控制</li>
 * </ol>
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    @Resource
    private StorageService storageService;
    @Resource
    private SysFileMapper sysFileMapper;

    /**
     * 上传文件并落元数据。
     *
     * <p>先按内容 MD5 查重：命中说明同一份文件已经存过，直接复用既有 objectKey
     * 并增加引用计数，不再占用存储空间和带宽（秒传）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public SysFile upload(MultipartFile file, FileUploadRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_EMPTY);
        }
        FileUploadRequest req = request == null ? new FileUploadRequest() : request;
        String bizType = StorageKeyGenerator.normalizeBizType(req.getBizType());
        String scope = StorageScope.normalize(req.getScope());

        String md5 = computeMd5(file);

        SysFile existing = findReusable(md5, file.getSize());
        if (existing != null) {
            int refCount = existing.getRefCount() == null ? 1 : existing.getRefCount();
            existing.setRefCount(refCount + 1);
            sysFileMapper.updateById(existing);
            log.info("命中秒传，复用已有文件：{}（objectKey={}）", existing.getOriginalName(), existing.getObjectKey());
            return fillUrl(existing);
        }

        StoredFile stored = storageService.upload(file, StorageContext.of(bizType, scope));

        SysFile record = new SysFile();
        record.setObjectKey(stored.getObjectKey());
        record.setOriginalName(StringUtils.hasText(stored.getOriginalName()) ? stored.getOriginalName() : "unnamed");
        record.setFileSuffix(StorageKeyGenerator.extractExtension(stored.getOriginalName()));
        record.setContentType(stored.getContentType());
        record.setFileSize(stored.getSize());
        record.setMd5(md5);
        record.setStorageType(stored.getStorageType());
        record.setStorageChannel(stored.getStorageChannel());
        record.setBizType(bizType);
        record.setBizId(req.getBizId());
        record.setScope(scope);
        record.setRefCount(1);
        fillUploader(record);
        sysFileMapper.insert(record);

        log.info("文件上传完成：{} -> {}（存储：{}/{}）", record.getOriginalName(),
                record.getObjectKey(), record.getStorageType(), record.getStorageChannel());
        return fillUrl(record);
    }

    /** 按内容指纹判断是否可以秒传，命中则返回已存在的记录 */
    public SysFile findReusable(String md5, Long size) {
        if (!StringUtils.hasText(md5)) {
            return null;
        }
        return sysFileMapper.selectOne(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getMd5, md5)
                .eq(size != null, SysFile::getFileSize, size)
                .orderByAsc(SysFile::getId)
                .last("limit 1"));
    }

    /** 分页查询文件，可按业务分类 / 业务主键 / 部门 / 存储类型筛选 */
    public PageResult<SysFile> page(FileQuery query) {
        FileQuery q = query == null ? new FileQuery() : query;
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<SysFile>()
                .eq(StringUtils.hasText(q.getBizType()), SysFile::getBizType, q.getBizType())
                .eq(q.getBizId() != null, SysFile::getBizId, q.getBizId())
                .eq(StringUtils.hasText(q.getScope()), SysFile::getScope, q.getScope())
                .eq(StringUtils.hasText(q.getDeptName()), SysFile::getDeptName, q.getDeptName())
                .eq(q.getUploaderId() != null, SysFile::getUploaderId, q.getUploaderId())
                .eq(StringUtils.hasText(q.getStorageType()), SysFile::getStorageType, q.getStorageType())
                .like(StringUtils.hasText(q.getOriginalName()), SysFile::getOriginalName, q.getOriginalName())
                .orderByDesc(SysFile::getId);
        Page<SysFile> page = sysFileMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), wrapper);
        page.getRecords().forEach(this::fillUrl);
        return PageResult.of(page);
    }

    /** 查询某个业务对象（如一台资产、一个工具）名下的全部文件 */
    public List<SysFile> listByBiz(String bizType, Long bizId) {
        List<SysFile> files = sysFileMapper.selectList(new LambdaQueryWrapper<SysFile>()
                .eq(StringUtils.hasText(bizType), SysFile::getBizType,
                        StorageKeyGenerator.normalizeBizType(bizType))
                .eq(bizId != null, SysFile::getBizId, bizId)
                .orderByDesc(SysFile::getId));
        files.forEach(this::fillUrl);
        return files;
    }

    /**
     * 删除文件。
     *
     * <p>引用计数大于 1 时只减计数（还有其他业务在用），归零才真正删掉物理文件。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        SysFile record = sysFileMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        int refCount = record.getRefCount() == null ? 1 : record.getRefCount();
        if (refCount > 1) {
            record.setRefCount(refCount - 1);
            sysFileMapper.updateById(record);
            log.info("文件仍被其他业务引用，仅减少引用计数：{} -> {}", record.getObjectKey(), record.getRefCount());
            return;
        }
        storageService.delete(record.getObjectKey());
        sysFileMapper.deleteById(id);
        log.info("文件已删除：{}", record.getObjectKey());
    }

    /** 按当前存储配置生成访问地址 */
    public String getUrl(String objectKey) {
        return StringUtils.hasText(objectKey) ? storageService.getUrl(objectKey) : null;
    }

    /** 当前生效的存储类型与渠道，便于排查配置 */
    public String currentChannel() {
        return storageService.getType().name().toLowerCase() + "/" + storageService.getChannel();
    }

    /**
     * 按存储后端统计文件数量与体积。
     *
     * <p>从本地迁到 OSS 的过程中，用它能直观看到进度（还剩多少文件在老后端）。</p>
     */
    public List<Map<String, Object>> statisticByStorage() {
        QueryWrapper<SysFile> wrapper = new QueryWrapper<>();
        wrapper.select("storage_type AS storageType", "storage_channel AS storageChannel",
                        "COUNT(*) AS fileCount", "IFNULL(SUM(file_size), 0) AS totalSize")
                .groupBy("storage_type", "storage_channel");
        return sysFileMapper.selectMaps(wrapper);
    }

    private String computeMd5(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return DigestUtils.md5DigestAsHex(in);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.STORAGE_ERROR, "计算文件指纹失败：" + e.getMessage());
        }
    }

    /** 记录上传人与归属部门，作为后续按部门检索、审计的依据 */
    private void fillUploader(SysFile record) {
        try {
            LoginUser loginUser = SecurityUtil.getLoginUser();
            SysUser user = loginUser.getUser();
            record.setUploaderId(user.getId());
            record.setUploaderName(StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername());
            record.setDeptName(user.getDepartment());
        } catch (BusinessException e) {
            // 无登录上下文的系统内部导入场景，允许上传人留空
            log.debug("当前无登录上下文，跳过上传人记录");
        }
    }

    private SysFile fillUrl(SysFile record) {
        if (record != null && StringUtils.hasText(record.getObjectKey())) {
            record.setUrl(storageService.getUrl(record.getObjectKey()));
        }
        return record;
    }
}
