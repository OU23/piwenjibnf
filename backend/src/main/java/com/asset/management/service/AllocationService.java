package com.asset.management.service;

import com.asset.management.common.constant.AllocationTypeEnum;
import com.asset.management.common.constant.AssetStatusEnum;
import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.ResultCode;
import com.asset.management.dto.AllocationDTO;
import com.asset.management.entity.Asset;
import com.asset.management.entity.AssetAllocation;
import com.asset.management.entity.SysUser;
import com.asset.management.mapper.AssetAllocationMapper;
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
 * 资产分配服务：手动分配、自动分配、领用确认
 */
@Service
public class AllocationService {

    @Resource
    private AssetAllocationMapper allocationMapper;
    @Resource
    private AssetMapper assetMapper;
    @Resource
    private SysUserMapper userMapper;
    @Resource
    private AssetService assetService;
    @Resource
    private CategoryService categoryService;

    /**
     * 手动分配：将指定资产分配给用户（在库 / 已申请 -> 已分配）
     * 整体事务，任一资产失败则全部回滚，保证原子性
     */
    @Transactional(rollbackFor = Exception.class)
    public int allocate(AllocationDTO dto) {
        SysUser target = userMapper.selectById(dto.getToUserId());
        if (target == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        String type = StringUtils.hasText(dto.getAllocationType())
                ? dto.getAllocationType() : AllocationTypeEnum.MANUAL.getCode();

        for (Long assetId : dto.getAssetIds()) {
            Asset asset = assetMapper.selectById(assetId);
            if (asset == null) {
                throw new BusinessException(ResultCode.ASSET_NOT_FOUND);
            }
            Long fromUserId = asset.getCustodianId();
            assetService.transitionStatus(assetId,
                    Set.of(AssetStatusEnum.IN_STOCK.getCode(), AssetStatusEnum.APPLIED.getCode()),
                    AssetStatusEnum.ALLOCATED.getCode(), dto.getToUserId());

            AssetAllocation allocation = new AssetAllocation();
            allocation.setAllocationNo(BizNoGenerator.generateAllocationNo());
            allocation.setAssetId(assetId);
            allocation.setFromUserId(fromUserId);
            allocation.setToUserId(dto.getToUserId());
            allocation.setAllocationType(type);
            allocation.setStatus(AssetStatusEnum.ALLOCATED.getCode());
            allocation.setRemark(dto.getRemark());
            allocation.setAllocateTime(LocalDateTime.now());
            allocationMapper.insert(allocation);
        }
        return dto.getAssetIds().size();
    }

    /** 单次自动分配数量上限，防止一次请求耗尽库存或压垮数据库 */
    private static final int MAX_AUTO_ALLOCATE_QUANTITY = 100;

    /**
     * 自动分配：按分类（含后代分类）自动挑选指定数量的【在库】资产分配给用户。
     * 采用乐观锁，冲突的资产自动跳过，返回实际分配数量。
     */
    @Transactional(rollbackFor = Exception.class)
    public int autoAllocate(Long categoryId, int quantity, Long toUserId, String remark) {
        if (categoryId == null || toUserId == null) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "请选择资产分类与分配对象");
        }
        if (quantity < 1) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "分配数量必须大于 0");
        }
        if (quantity > MAX_AUTO_ALLOCATE_QUANTITY) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "单次自动分配数量不能超过 " + MAX_AUTO_ALLOCATE_QUANTITY);
        }
        SysUser target = userMapper.selectById(toUserId);
        if (target == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        List<Long> categoryIds = categoryService.listCategoryIdsWithDescendants(categoryId);
        // 使用分页插件限制查询条数，避免 .last() 手工拼接 LIMIT
        Page<Asset> candidatePage = assetMapper.selectPage(new Page<>(1, quantity),
                new LambdaQueryWrapper<Asset>()
                        .in(Asset::getCategoryId, categoryIds)
                        .eq(Asset::getStatus, AssetStatusEnum.IN_STOCK.getCode())
                        .orderByAsc(Asset::getId));
        List<Asset> candidates = candidatePage.getRecords();

        int allocated = 0;
        for (Asset candidate : candidates) {
            try {
                assetService.transitionStatus(candidate.getId(),
                        Set.of(AssetStatusEnum.IN_STOCK.getCode()),
                        AssetStatusEnum.ALLOCATED.getCode(), toUserId);
            } catch (BusinessException e) {
                if (e.getCode() == ResultCode.DATA_INCONSISTENT.getCode()) {
                    continue; // 并发冲突，跳过该资产
                }
                throw e;
            }

            AssetAllocation allocation = new AssetAllocation();
            allocation.setAllocationNo(BizNoGenerator.generateAllocationNo());
            allocation.setAssetId(candidate.getId());
            allocation.setFromUserId(null);
            allocation.setToUserId(toUserId);
            allocation.setAllocationType(AllocationTypeEnum.AUTO.getCode());
            allocation.setStatus(AssetStatusEnum.ALLOCATED.getCode());
            allocation.setRemark(remark);
            allocation.setAllocateTime(LocalDateTime.now());
            allocationMapper.insert(allocation);
            allocated++;
        }
        return allocated;
    }

    /**
     * 领用确认：已分配 -> 使用中（仅分配对象本人）
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long allocationId, Long userId) {
        AssetAllocation allocation = allocationMapper.selectById(allocationId);
        if (allocation == null) {
            throw new BusinessException(ResultCode.ALLOCATION_NOT_FOUND);
        }
        if (!userId.equals(allocation.getToUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能确认分配给自己的资产");
        }
        if (!AssetStatusEnum.ALLOCATED.getCode().equals(allocation.getStatus())) {
            throw new BusinessException(ResultCode.ASSET_STATUS_INVALID.getCode(), "该分配记录已处理");
        }
        assetService.transitionStatus(allocation.getAssetId(),
                Set.of(AssetStatusEnum.ALLOCATED.getCode()),
                AssetStatusEnum.IN_USE.getCode(), userId);
        allocation.setStatus(AssetStatusEnum.IN_USE.getCode());
        allocationMapper.updateById(allocation);
    }

    public PageResult<AssetAllocation> page(long pageNum, long pageSize, Long assetId, Long toUserId, String status) {
        LambdaQueryWrapper<AssetAllocation> wrapper = new LambdaQueryWrapper<AssetAllocation>()
                .eq(assetId != null, AssetAllocation::getAssetId, assetId)
                .eq(toUserId != null, AssetAllocation::getToUserId, toUserId)
                .eq(StringUtils.hasText(status), AssetAllocation::getStatus, status)
                .orderByDesc(AssetAllocation::getId);
        Page<AssetAllocation> page = allocationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        enrich(page.getRecords());
        return PageResult.of(page);
    }

    /**
     * 查询用户待领用的分配记录
     */
    public List<AssetAllocation> listPendingByUser(Long userId) {
        List<AssetAllocation> list = allocationMapper.selectList(new LambdaQueryWrapper<AssetAllocation>()
                .eq(AssetAllocation::getToUserId, userId)
                .eq(AssetAllocation::getStatus, AssetStatusEnum.ALLOCATED.getCode())
                .orderByDesc(AssetAllocation::getId));
        enrich(list);
        return list;
    }

    private void enrich(List<AssetAllocation> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            return;
        }
        List<Long> assetIds = allocations.stream().map(AssetAllocation::getAssetId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> userIds = allocations.stream()
                .flatMap(a -> java.util.stream.Stream.of(a.getFromUserId(), a.getToUserId()))
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, Asset> assetMap = assetIds.isEmpty() ? Collections.emptyMap()
                : assetMapper.selectBatchIds(assetIds).stream()
                .collect(Collectors.toMap(Asset::getId, a -> a, (a, b) -> a));
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(), (a, b) -> a));

        allocations.forEach(allocation -> {
            Asset asset = assetMap.get(allocation.getAssetId());
            if (asset != null) {
                allocation.setAssetName(asset.getAssetName());
                allocation.setAssetCode(asset.getAssetCode());
            }
            allocation.setToUserName(userMap.get(allocation.getToUserId()));
            allocation.setFromUserName(userMap.get(allocation.getFromUserId()));
        });
    }
}
