package com.asset.management.service;

import com.asset.management.common.constant.AssetStatusEnum;
import com.asset.management.common.constant.BusinessTypeEnum;
import com.asset.management.common.constant.CommonStatus;
import com.asset.management.common.constant.ReturnStatusEnum;
import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.ResultCode;
import com.asset.management.dto.ReturnDTO;
import com.asset.management.entity.ApprovalRecord;
import com.asset.management.entity.Asset;
import com.asset.management.entity.AssetReturn;
import com.asset.management.entity.SysUser;
import com.asset.management.mapper.ApprovalRecordMapper;
import com.asset.management.mapper.AssetMapper;
import com.asset.management.mapper.AssetReturnMapper;
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
 * 资产归还服务：归还登记、归还审核
 */
@Service
public class ReturnService {

    @Resource
    private AssetReturnMapper returnMapper;
    @Resource
    private AssetMapper assetMapper;
    @Resource
    private SysUserMapper userMapper;
    @Resource
    private ApprovalRecordMapper approvalRecordMapper;
    @Resource
    private AssetService assetService;

    /**
     * 提交归还：仅资产当前使用人可归还（使用中 -> 待归还）
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(ReturnDTO dto, Long userId) {
        Asset asset = assetMapper.selectById(dto.getAssetId());
        if (asset == null) {
            throw new BusinessException(ResultCode.ASSET_NOT_FOUND);
        }
        if (!userId.equals(asset.getCustodianId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能归还自己名下的资产");
        }
        assetService.transitionStatus(asset.getId(),
                Set.of(AssetStatusEnum.IN_USE.getCode()),
                AssetStatusEnum.PENDING_RETURN.getCode(), null);

        AssetReturn assetReturn = new AssetReturn();
        assetReturn.setReturnNo(BizNoGenerator.generateReturnNo());
        assetReturn.setAssetId(dto.getAssetId());
        assetReturn.setUserId(userId);
        assetReturn.setReason(dto.getReason());
        assetReturn.setStatus(ReturnStatusEnum.PENDING.getCode());
        assetReturn.setReturnTime(LocalDateTime.now());
        returnMapper.insert(assetReturn);
    }

    /**
     * 归还审核：通过（待归还 -> 在库）/ 驳回（待归还 -> 使用中）
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long returnId, Integer result, String comment, Long auditorId) {
        AssetReturn assetReturn = returnMapper.selectById(returnId);
        if (assetReturn == null) {
            throw new BusinessException(ResultCode.RETURN_NOT_FOUND);
        }
        if (!ReturnStatusEnum.PENDING.getCode().equals(assetReturn.getStatus())) {
            throw new BusinessException(ResultCode.RETURN_STATUS_INVALID);
        }

        if (result != null && result == CommonStatus.APPROVED) {
            assetService.transitionStatusAndClearCustodian(assetReturn.getAssetId(),
                    Set.of(AssetStatusEnum.PENDING_RETURN.getCode()),
                    AssetStatusEnum.IN_STOCK.getCode());
            assetReturn.setStatus(ReturnStatusEnum.COMPLETED.getCode());
        } else {
            assetService.transitionStatus(assetReturn.getAssetId(),
                    Set.of(AssetStatusEnum.PENDING_RETURN.getCode()),
                    AssetStatusEnum.IN_USE.getCode(), assetReturn.getUserId());
            assetReturn.setStatus(ReturnStatusEnum.REJECTED.getCode());
        }

        assetReturn.setAuditRemark(comment);
        assetReturn.setAuditTime(LocalDateTime.now());
        assetReturn.setAuditorId(auditorId);
        returnMapper.updateById(assetReturn);

        saveApprovalRecord(returnId, result == null ? CommonStatus.REJECTED : result, comment, auditorId);
    }

    public PageResult<AssetReturn> page(long pageNum, long pageSize, String status, Long userId) {
        LambdaQueryWrapper<AssetReturn> wrapper = new LambdaQueryWrapper<AssetReturn>()
                .eq(StringUtils.hasText(status), AssetReturn::getStatus, status)
                .eq(userId != null, AssetReturn::getUserId, userId)
                .orderByDesc(AssetReturn::getId);
        Page<AssetReturn> page = returnMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        enrich(page.getRecords());
        return PageResult.of(page);
    }

    /**
     * 查询归还单审批流水
     */
    public List<ApprovalRecord> listApprovalRecords(Long businessId) {
        List<ApprovalRecord> records = approvalRecordMapper.selectList(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getBusinessType, BusinessTypeEnum.RETURN.getCode())
                        .eq(ApprovalRecord::getBusinessId, businessId)
                        .orderByDesc(ApprovalRecord::getApproveTime));
        if (!records.isEmpty()) {
            List<Long> approverIds = records.stream().map(ApprovalRecord::getApproverId)
                    .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, String> userMap = userMapper.selectBatchIds(approverIds).stream()
                    .collect(Collectors.toMap(SysUser::getId,
                            u -> StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(), (a, b) -> a));
            records.forEach(r -> r.setApproverName(userMap.get(r.getApproverId())));
        }
        return records;
    }

    private void saveApprovalRecord(Long businessId, int result, String comment, Long approverId) {
        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType(BusinessTypeEnum.RETURN.getCode());
        record.setBusinessId(businessId);
        record.setApproverId(approverId);
        record.setResult(result);
        record.setComment(comment);
        record.setApproveTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);
    }

    private void enrich(List<AssetReturn> returns) {
        if (returns == null || returns.isEmpty()) {
            return;
        }
        List<Long> assetIds = returns.stream().map(AssetReturn::getAssetId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> userIds = returns.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getUserId(), r.getAuditorId()))
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, Asset> assetMap = assetIds.isEmpty() ? Collections.emptyMap()
                : assetMapper.selectBatchIds(assetIds).stream()
                .collect(Collectors.toMap(Asset::getId, a -> a, (a, b) -> a));
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(), (a, b) -> a));

        returns.forEach(r -> {
            Asset asset = assetMap.get(r.getAssetId());
            if (asset != null) {
                r.setAssetName(asset.getAssetName());
                r.setAssetCode(asset.getAssetCode());
            }
            r.setUserName(userMap.get(r.getUserId()));
            r.setAuditorName(userMap.get(r.getAuditorId()));
        });
    }
}
