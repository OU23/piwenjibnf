package com.asset.management.controller;

import com.asset.management.common.annotation.OperationLog;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.Result;
import com.asset.management.common.result.ResultCode;
import com.asset.management.dto.FileQuery;
import com.asset.management.dto.FileUploadRequest;
import com.asset.management.entity.SysFile;
import com.asset.management.service.FileService;
import com.asset.management.storage.StorageService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件存储接口。
 *
 * <p>统一的文件出入口：业务方上传后拿到 objectKey 存进自己的业务表，
 * 展示时用 {@code /api/file/preview/{objectKey}} 访问。存储后端是本地还是 OSS，
 * 对调用方完全透明。</p>
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    /** 预览路径中用于定位 objectKey 的标记 */
    private static final String PREVIEW_MARKER = "/preview/";

    @Resource
    private FileService fileService;
    @Resource
    private StorageService storageService;

    /**
     * 上传文件。
     *
     * <p>bizType 是扩展入口：以后每新增一类工具（巡检、耗材、合同……），
     * 传一个新的 bizType 即可，存储层无需任何改动。</p>
     */
    @OperationLog(module = "文件管理", type = Constants.OPERATION_INSERT)
    @PostMapping("/upload")
    public Result<SysFile> upload(@RequestParam("file") MultipartFile file,
                                  @ModelAttribute FileUploadRequest request) {
        return Result.success("上传成功", fileService.upload(file, request));
    }

    /** 秒传校验：前端上传前先用文件 MD5 探一下，命中就不用再传了 */
    @PostMapping("/check")
    public Result<SysFile> check(@RequestParam String md5,
                                 @RequestParam(required = false) Long size) {
        return Result.success(fileService.findReusable(md5, size));
    }

    /** 分页查询文件（按业务分类 / 业务主键 / 部门 / 存储类型筛选） */
    @GetMapping("/page")
    public Result<PageResult<SysFile>> page(FileQuery query) {
        return Result.success(fileService.page(query));
    }

    /** 查询某个业务对象名下的全部文件 */
    @GetMapping("/biz")
    public Result<List<SysFile>> listByBiz(@RequestParam String bizType,
                                           @RequestParam(required = false) Long bizId) {
        return Result.success(fileService.listByBiz(bizType, bizId));
    }

    /** 删除文件（引用计数归零才会真正删物理文件） */
    @OperationLog(module = "文件管理", type = Constants.OPERATION_DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.remove(id);
        return Result.success();
    }

    /**
     * 预览 / 下载。
     *
     * <p>该路径在 SecurityConfig 中放行 —— 浏览器的 img、video 标签没法带 JWT，
     * 靠 objectKey 中的 UUID 保证不可枚举。</p>
     *
     * @param download 传 true 时作为附件下载，否则浏览器内联预览
     */
    @GetMapping("/preview/**")
    public ResponseEntity<InputStreamResource> preview(HttpServletRequest request) {
        String objectKey = resolveObjectKey(request);
        InputStream in = storageService.download(objectKey);

        MediaType mediaType = MediaTypeFactory.getMediaType(objectKey)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        boolean download = "true".equalsIgnoreCase(request.getParameter("download"));
        String fileName = objectKey.substring(objectKey.lastIndexOf('/') + 1);
        ContentDisposition disposition = (download
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(in));
    }

    /** 当前生效的存储类型与渠道，便于排查配置 */
    @GetMapping("/storage")
    public Result<Map<String, Object>> storage() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("type", storageService.getType().name().toLowerCase());
        info.put("channel", storageService.getChannel());
        return Result.success(info);
    }

    /** 按存储后端统计文件数量与体积，用于观察迁移进度 */
    @GetMapping("/statistic")
    public Result<List<Map<String, Object>>> statistic() {
        return Result.success(fileService.statisticByStorage());
    }

    private String resolveObjectKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int index = uri.indexOf(PREVIEW_MARKER);
        if (index < 0) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        String objectKey = URLDecoder.decode(uri.substring(index + PREVIEW_MARKER.length()),
                StandardCharsets.UTF_8);
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        return objectKey;
    }
}
