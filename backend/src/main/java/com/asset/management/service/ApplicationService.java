package com.asset.management.service;

import com.asset.management.common.constant.ApplicationStatusEnum;
import com.asset.management.common.constant.AssetStatusEnum;
import com.asset.management.common.constant.BusinessTypeEnum;
import com.asset.management.common.constant.CommonStatus;
import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.ResultCode;
import com.asset.management.dto.ApplicationDTO;
import com.asset.management.entity.ApprovalRecord;
import com.asset.management.entity.Asset;
import com.asset.management.entity.AssetApplication;
import com.asset.management.entity.AssetCategory;
import com.asset.management.entity.SysUser;
import com.asset.management.mapper.ApprovalRecordMapper;
import com.asset.management.mapper.AssetApplicationMapper;
import com.asset.management.mapper.AssetCategoryMapper;
import com.asset.management.mapper.AssetMapper;
import com.asset.management.mapper.SysUserMapper;
import com.asset.management.util.BizNoGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资产申请服务：申请提交、审批流转、撤销
 */
@Service
public class ApplicationService {

    @Resource
    private AssetApplicationMapper applicationMapper;
    @Resource
    private AssetMapper assetMapper;
    @Resource
    private AssetCategoryMapper categoryMapper;
    @Resource
    private SysUserMapper userMapper;
    @Resource
    private ApprovalRecordMapper approvalRecordMapper;
    @Resource
    private AssetService assetService;

    /**
     * 提交申请
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(ApplicationDTO dto, Long applicantId) {
        AssetCategory category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "申请分类不存在");
        }

        int quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        if (dto.getAssetId() != null) {
            // 指定具体资产
            Asset asset = assetMapper.selectById(dto.getAssetId());
            if (asset == null) {
                throw new BusinessException(ResultCode.ASSET_NOT_FOUND);
            }
            if (!AssetStatusEnum.IN_STOCK.getCode().equals(asset.getStatus())) {
                throw new BusinessException(ResultCode.ASSET_STATUS_INVALID.getCode(), "该资产当前不可申请");
            }
            quantity = 1;
        }
        if (quantity < 1) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "申请数量必须大于 0");
        }

        AssetApplication application = new AssetApplication();
        application.setApplicationNo(BizNoGenerator.generateApplicationNo());
        application.setApplicantId(applicantId);
        application.setCategoryId(dto.getCategoryId());
        application.setAssetId(dto.getAssetId());
        application.setQuantity(quantity);
        application.setReason(dto.getReason());
        application.setStatus(ApplicationStatusEnum.PENDING.getCode());
        application.setApplyTime(LocalDateTime.now());
        applicationMapper.insert(application);
    }

    /**
     * 审批申请：通过 / 驳回
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long applicationId, Integer result, String comment, Long approverId) {
        AssetApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ResultCode.APPLICATION_NOT_FOUND);
        }
        if (!ApplicationStatusEnum.PENDING.getCode().equals(application.getStatus())) {
            throw new BusinessException(ResultCode.APPLICATION_STATUS_INVALID);
        }

        if (result != null && result == CommonStatus.APPROVED) {
            // 指定资产的申请：审批通过即占用资产（在库 -> 已申请）
            if (application.getAssetId() != null) {
                assetService.transitionStatus(application.getAssetId(),
                        Set.of(AssetStatusEnum.IN_STOCK.getCode()),
                        AssetStatusEnum.APPLIED.getCode(), application.getApplicantId());
            }
            application.setStatus(ApplicationStatusEnum.APPROVED.getCode());
        } else {
            application.setStatus(ApplicationStatusEnum.REJECTED.getCode());
        }

        application.setHandleRemark(comment);
        application.setHandleTime(LocalDateTime.now());
        application.setHandlerId(approverId);
        applicationMapper.updateById(application);

        saveApprovalRecord(applicationId, result == null ? CommonStatus.REJECTED : result, comment, approverId);
    }

    /**
     * 撤销申请（仅申请人本人，待审批或已通过状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Long userId) {
        AssetApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException(ResultCode.APPLICATION_NOT_FOUND);
        }
        if (!application.getApplicantId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能撤销自己的申请");
        }
        String status = application.getStatus();
        if (!ApplicationStatusEnum.PENDING.getCode().equals(status)
                && !ApplicationStatusEnum.APPROVED.getCode().equals(status)) {
            throw new BusinessException(ResultCode.APPLICATION_STATUS_INVALID);
        }

        // 已通过且指定资产：释放占用（已申请 -> 在库）
        if (ApplicationStatusEnum.APPROVED.getCode().equals(status) && application.getAssetId() != null) {
            Asset asset = assetMapper.selectById(application.getAssetId());
            if (asset != null
                    && AssetStatusEnum.APPLIED.getCode().equals(asset.getStatus())
                    && userId.equals(asset.getCustodianId())) {
                assetService.transitionStatus(application.getAssetId(),
                        Set.of(AssetStatusEnum.APPLIED.getCode()),
                        AssetStatusEnum.IN_STOCK.getCode(), null);
            }
        }

        application.setStatus(ApplicationStatusEnum.CANCELLED.getCode());
        application.setHandleTime(LocalDateTime.now());
        applicationMapper.updateById(application);
    }

    public PageResult<AssetApplication> page(long pageNum, long pageSize, String status, Long applicantId,
                                            String keyword) {
        LambdaQueryWrapper<AssetApplication> wrapper = new LambdaQueryWrapper<AssetApplication>()
                .eq(StringUtils.hasText(status), AssetApplication::getStatus, status)
                .eq(applicantId != null, AssetApplication::getApplicantId, applicantId)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(AssetApplication::getApplicationNo, keyword)
                        .or()
                        .like(AssetApplication::getReason, keyword))
                .orderByDesc(AssetApplication::getId);
        Page<AssetApplication> page = applicationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        enrich(page.getRecords());
        return PageResult.of(page);
    }

    /**
     * 查询某申请单的审批流水
     */
    public List<ApprovalRecord> listApprovalRecords(Long businessId) {
        List<ApprovalRecord> records = approvalRecordMapper.selectList(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getBusinessType, BusinessTypeEnum.APPLICATION.getCode())
                        .eq(ApprovalRecord::getBusinessId, businessId)
                        .orderByDesc(ApprovalRecord::getApproveTime));
        enrichApprovalRecords(records);
        return records;
    }

    private void saveApprovalRecord(Long businessId, int result, String comment, Long approverId) {
        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType(BusinessTypeEnum.APPLICATION.getCode());
        record.setBusinessId(businessId);
        record.setApproverId(approverId);
        record.setResult(result);
        record.setComment(comment);
        record.setApproveTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);
    }

    private void enrich(List<AssetApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return;
        }
        List<Long> userIds = applications.stream()
                .flatMap(a -> java.util.stream.Stream.of(a.getApplicantId(), a.getHandlerId()))
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> categoryIds = applications.stream().map(AssetApplication::getCategoryId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> assetIds = applications.stream().map(AssetApplication::getAssetId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(), (a, b) -> a));
        Map<Long, String> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(AssetCategory::getId, AssetCategory::getCategoryName, (a, b) -> a));
        Map<Long, Asset> assetMap = assetIds.isEmpty() ? Collections.emptyMap()
                : assetMapper.selectBatchIds(assetIds).stream()
                .collect(Collectors.toMap(Asset::getId, a -> a, (a, b) -> a));

        applications.forEach(app -> {
            app.setApplicantName(userMap.get(app.getApplicantId()));
            app.setHandlerName(userMap.get(app.getHandlerId()));
            app.setCategoryName(categoryMap.get(app.getCategoryId()));
            if (app.getAssetId() != null && assetMap.get(app.getAssetId()) != null) {
                Asset asset = assetMap.get(app.getAssetId());
                app.setAssetName(asset.getAssetName());
                app.setAssetCode(asset.getAssetCode());
            }
        });
    }

    private void enrichApprovalRecords(List<ApprovalRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> approverIds = records.stream().map(ApprovalRecord::getApproverId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> userMap = approverIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(approverIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(), (a, b) -> a));
        records.forEach(r -> r.setApproverName(userMap.get(r.getApproverId())));
    }
}
