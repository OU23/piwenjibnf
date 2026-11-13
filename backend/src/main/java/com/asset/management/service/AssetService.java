package com.asset.management.service;

import com.asset.management.common.constant.AssetStatusEnum;
import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.ResultCode;
import com.asset.management.dto.AssetDTO;
import com.asset.management.dto.AssetQuery;
import com.asset.management.entity.Asset;
import com.asset.management.entity.AssetCategory;
import com.asset.management.entity.SysUser;
import com.asset.management.mapper.AssetCategoryMapper;
import com.asset.management.mapper.AssetMapper;
import com.asset.management.mapper.SysUserMapper;
import com.asset.management.util.BizNoGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资产服务：全生命周期管理 + 状态流转
 */
@Service
public class AssetService {

    @Resource
    private AssetMapper assetMapper;
    @Resource
    private AssetCategoryMapper categoryMapper;
    @Resource
    private SysUserMapper userMapper;
    @Resource
    private CategoryService categoryService;

    public PageResult<Asset> page(AssetQuery query) {
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<Asset>()
                .like(StringUtils.hasText(query.getAssetName()), Asset::getAssetName, query.getAssetName())
                .like(StringUtils.hasText(query.getAssetCode()), Asset::getAssetCode, query.getAssetCode())
                .like(StringUtils.hasText(query.getBrand()), Asset::getBrand, query.getBrand())
                .eq(StringUtils.hasText(query.getStatus()), Asset::getStatus, query.getStatus())
                .eq(query.getCustodianId() != null, Asset::getCustodianId, query.getCustodianId())
                .orderByDesc(Asset::getId);

        // 按分类树过滤（含后代分类）
        if (query.getCategoryId() != null) {
            List<Long> categoryIds = categoryService.listCategoryIdsWithDescendants(query.getCategoryId());
            wrapper.in(Asset::getCategoryId, categoryIds);
        }

        Page<Asset> page = assetMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        enrich(page.getRecords());
        return PageResult.of(page);
    }

    public Asset getById(Long id) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new BusinessException(ResultCode.ASSET_NOT_FOUND);
        }
        enrich(Collections.singletonList(asset));
        return asset;
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(AssetDTO dto, Long operatorId) {
        String assetCode = dto.getAssetCode();
        if (!StringUtils.hasText(assetCode)) {
            assetCode = BizNoGenerator.generateAssetCode();
        } else {
            checkCodeUnique(assetCode, null);
        }
        Asset asset = new Asset();
        asset.setAssetCode(assetCode);
        asset.setAssetName(dto.getAssetName());
        asset.setCategoryId(dto.getCategoryId());
        asset.setBrand(dto.getBrand());
        asset.setModel(dto.getModel());
        asset.setSerialNumber(dto.getSerialNumber());
        asset.setPrice(dto.getPrice());
        asset.setPurchaseDate(dto.getPurchaseDate());
        asset.setLocation(dto.getLocation());
        asset.setRemark(dto.getRemark());
        asset.setStatus(AssetStatusEnum.IN_STOCK.getCode());
        asset.setCreateBy(operatorId);
        asset.setVersion(0);
        assetMapper.insert(asset);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(AssetDTO dto) {
        Asset asset = assetMapper.selectById(dto.getId());
        if (asset == null) {
            throw new BusinessException(ResultCode.ASSET_NOT_FOUND);
        }
        if (StringUtils.hasText(dto.getAssetCode()) && !dto.getAssetCode().equals(asset.getAssetCode())) {
            checkCodeUnique(dto.getAssetCode(), dto.getId());
            asset.setAssetCode(dto.getAssetCode());
        }
        asset.setAssetName(dto.getAssetName());
        asset.setCategoryId(dto.getCategoryId());
        asset.setBrand(dto.getBrand());
        asset.setModel(dto.getModel());
        asset.setSerialNumber(dto.getSerialNumber());
        asset.setPrice(dto.getPrice());
        asset.setPurchaseDate(dto.getPurchaseDate());
        asset.setLocation(dto.getLocation());
        asset.setRemark(dto.getRemark());
        assetMapper.updateById(asset);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new BusinessException(ResultCode.ASSET_NOT_FOUND);
        }
        if (!AssetStatusEnum.IN_STOCK.getCode().equals(asset.getStatus())
                && !AssetStatusEnum.SCRAPPED.getCode().equals(asset.getStatus())) {
            throw new BusinessException(ResultCode.ASSET_STATUS_INVALID.getCode(), "仅【在库】或【已报废】状态的资产允许删除");
        }
        assetMapper.deleteById(id);
    }

    /**
     * 报废：仅【在库】或【使用中】可报废
     */
    @Transactional(rollbackFor = Exception.class)
    public void scrap(Long id) {
        transitionStatus(id,
                Set.of(AssetStatusEnum.IN_STOCK.getCode(), AssetStatusEnum.IN_USE.getCode(), AssetStatusEnum.REPAIRING.getCode()),
                AssetStatusEnum.SCRAPPED.getCode(), null);
    }

    /**
     * 报修：仅【使用中】可报修
     */
    @Transactional(rollbackFor = Exception.class)
    public void repair(Long id) {
        transitionStatus(id, Set.of(AssetStatusEnum.IN_USE.getCode()),
                AssetStatusEnum.REPAIRING.getCode(), null);
    }

    /**
     * 维修完成：仅【维修中】可恢复为【使用中】
     */
    @Transactional(rollbackFor = Exception.class)
    public void repairDone(Long id) {
        transitionStatus(id, Set.of(AssetStatusEnum.REPAIRING.getCode()),
                AssetStatusEnum.IN_USE.getCode(), null);
    }

    /**
     * 原子状态流转（供申请、分配、归还等业务调用）：
     * 校验当前状态是否在允许范围内，再更新状态，乐观锁保证并发安全。
     *
     * @param assetId     资产 ID
     * @param allowedFrom 允许的当前状态集合
     * @param target      目标状态
     * @param custodianId 更新后的保管人 ID（可为 null 表示不变）
     */
    public void transitionStatus(Long assetId, Set<String> allowedFrom, String target, Long custodianId) {
        Asset asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw new BusinessException(ResultCode.ASSET_NOT_FOUND);
        }
        if (!allowedFrom.contains(asset.getStatus())) {
            throw new BusinessException(ResultCode.ASSET_STATUS_INVALID.getCode(),
                    "资产当前状态为【" + AssetStatusEnum.getDesc(asset.getStatus()) + "】，不允许执行该操作");
        }
        // 注意：此处依赖 MyBatis-Plus 乐观锁插件（@Version）生效。
        // asset 由 selectById 读出，version 非空，updateById 时插件自动带 WHERE version=N 并置 version=N+1。
        // 若并发下他人已更新，rows=0，判定为数据不一致。切勿在此处复用 version 为 null 的对象，否则乐观锁会静默失效。
        asset.setStatus(target);
        if (custodianId != null) {
            asset.setCustodianId(custodianId);
        }
        int rows = assetMapper.updateById(asset);
        if (rows == 0) {
            throw new BusinessException(ResultCode.DATA_INCONSISTENT);
        }
    }

    /**
     * 状态流转并清空保管人（归还完成场景：待归还 -> 在库）
     */
    public void transitionStatusAndClearCustodian(Long assetId, Set<String> allowedFrom, String target) {
        Asset asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw new BusinessException(ResultCode.ASSET_NOT_FOUND);
        }
        if (!allowedFrom.contains(asset.getStatus())) {
            throw new BusinessException(ResultCode.ASSET_STATUS_INVALID.getCode(),
                    "资产当前状态为【" + AssetStatusEnum.getDesc(asset.getStatus()) + "】，不允许执行该操作");
        }
        // 显式将 custodian_id 置空（默认更新策略会忽略 null 字段），并借助 version 乐观锁保证并发安全
        int rows = assetMapper.update(null, new LambdaUpdateWrapper<Asset>()
                .eq(Asset::getId, assetId)
                .eq(Asset::getVersion, asset.getVersion())
                .set(Asset::getStatus, target)
                .set(Asset::getCustodianId, null)
                .set(Asset::getVersion, asset.getVersion() + 1));
        if (rows == 0) {
            throw new BusinessException(ResultCode.DATA_INCONSISTENT);
        }
    }

    /**
     * 批量查询资产（供分配模块使用）
     */
    public List<Asset> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return assetMapper.selectBatchIds(ids);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<Asset>().eq(Asset::getAssetCode, code);
        if (excludeId != null) {
            wrapper.ne(Asset::getId, excludeId);
        }
        if (assetMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.ASSET_CODE_EXISTS);
        }
    }

    /**
     * 批量填充分类名称与保管人姓名
     */
    private void enrich(List<Asset> assets) {
        if (assets == null || assets.isEmpty()) {
            return;
        }
        List<Long> categoryIds = assets.stream().map(Asset::getCategoryId).filter(java.util.Objects::nonNull)
                .distinct().collect(Collectors.toList());
        List<Long> userIds = assets.stream().map(Asset::getCustodianId).filter(java.util.Objects::nonNull)
                .distinct().collect(Collectors.toList());

        Map<Long, String> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(AssetCategory::getId, AssetCategory::getCategoryName, (a, b) -> a));
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(), (a, b) -> a));

        assets.forEach(asset -> {
            if (asset.getCategoryId() != null) {
                asset.setCategoryName(categoryMap.get(asset.getCategoryId()));
            }
            if (asset.getCustodianId() != null) {
                asset.setCustodianName(userMap.get(asset.getCustodianId()));
            }
        });
    }
}
