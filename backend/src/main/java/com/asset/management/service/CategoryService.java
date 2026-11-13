package com.asset.management.service;

import com.asset.management.common.constant.CommonStatus;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.ResultCode;
import com.asset.management.dto.CategoryDTO;
import com.asset.management.entity.Asset;
import com.asset.management.entity.AssetCategory;
import com.asset.management.mapper.AssetCategoryMapper;
import com.asset.management.mapper.AssetMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 资产分类服务：多级分类树管理
 */
@Service
public class CategoryService {

    @Resource
    private AssetCategoryMapper categoryMapper;
    @Resource
    private AssetMapper assetMapper;

    /**
     * 分类树
     */
    public List<CategoryDTO> listTree() {
        List<AssetCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<AssetCategory>().orderByAsc(AssetCategory::getSort));
        return buildTree(categories, Constants.ROOT_PARENT_ID);
    }

    /**
     * 全部分类（扁平，用于下拉选择）
     */
    public List<AssetCategory> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<AssetCategory>()
                .eq(AssetCategory::getStatus, CommonStatus.ENABLED)
                .orderByAsc(AssetCategory::getSort));
    }

    /**
     * 查询某分类及其所有后代分类的 ID 列表（用于按分类树过滤资产）
     */
    public List<Long> listCategoryIdsWithDescendants(Long categoryId) {
        List<AssetCategory> all = categoryMapper.selectList(null);
        List<Long> result = new ArrayList<>();
        result.add(categoryId);
        collectDescendants(all, categoryId, result);
        return result;
    }

    private void collectDescendants(List<AssetCategory> all, Long parentId, List<Long> result) {
        for (AssetCategory category : all) {
            if (parentId.equals(category.getParentId())) {
                result.add(category.getId());
                collectDescendants(all, category.getId(), result);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(CategoryDTO dto) {
        checkCodeUnique(dto.getCategoryCode(), null);
        AssetCategory category = new AssetCategory();
        applyDTO(category, dto);
        category.setLevel(calcLevel(dto.getParentId()));
        categoryMapper.insert(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(CategoryDTO dto) {
        AssetCategory category = categoryMapper.selectById(dto.getId());
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (dto.getId().equals(dto.getParentId())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "父分类不能为自身");
        }
        checkCodeUnique(dto.getCategoryCode(), dto.getId());
        applyDTO(category, dto);
        category.setLevel(calcLevel(dto.getParentId()));
        categoryMapper.updateById(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AssetCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<AssetCategory>().eq(AssetCategory::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ResultCode.CATEGORY_HAS_CHILDREN);
        }
        Long assetCount = assetMapper.selectCount(
                new LambdaQueryWrapper<Asset>().eq(Asset::getCategoryId, id));
        if (assetCount != null && assetCount > 0) {
            throw new BusinessException(ResultCode.CATEGORY_HAS_ASSET);
        }
        categoryMapper.deleteById(id);
    }

    private int calcLevel(Long parentId) {
        if (parentId == null || Constants.ROOT_PARENT_ID.equals(parentId)) {
            return 1;
        }
        AssetCategory parent = categoryMapper.selectById(parentId);
        return parent == null ? 1 : parent.getLevel() + 1;
    }

    private void applyDTO(AssetCategory category, CategoryDTO dto) {
        category.setParentId(dto.getParentId() == null ? Constants.ROOT_PARENT_ID : dto.getParentId());
        category.setCategoryName(dto.getCategoryName());
        category.setCategoryCode(dto.getCategoryCode());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setDescription(dto.getDescription());
        category.setAttributes(dto.getAttributes());
        category.setStatus(dto.getStatus() == null ? CommonStatus.ENABLED : dto.getStatus());
    }

    private List<CategoryDTO> buildTree(List<AssetCategory> categories, Long parentId) {
        List<CategoryDTO> result = new ArrayList<>();
        for (AssetCategory category : categories) {
            if (parentId.equals(category.getParentId())) {
                CategoryDTO dto = toDTO(category);
                dto.setChildren(buildTree(categories, category.getId()));
                result.add(dto);
            }
        }
        return result;
    }

    private CategoryDTO toDTO(AssetCategory category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setParentId(category.getParentId());
        dto.setCategoryName(category.getCategoryName());
        dto.setCategoryCode(category.getCategoryCode());
        dto.setSort(category.getSort());
        dto.setDescription(category.getDescription());
        dto.setAttributes(category.getAttributes());
        dto.setStatus(category.getStatus());
        return dto;
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<AssetCategory> wrapper =
                new LambdaQueryWrapper<AssetCategory>().eq(AssetCategory::getCategoryCode, code);
        if (excludeId != null) {
            wrapper.ne(AssetCategory::getId, excludeId);
        }
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.CATEGORY_CODE_EXISTS);
        }
    }
}
