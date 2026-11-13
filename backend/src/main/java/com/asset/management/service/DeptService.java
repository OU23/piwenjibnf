package com.asset.management.service;

import com.asset.management.common.constant.CommonStatus;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.PageResult;
import com.asset.management.common.result.ResultCode;
import com.asset.management.dto.DeptDTO;
import com.asset.management.entity.SysDept;
import com.asset.management.entity.SysUser;
import com.asset.management.mapper.SysDeptMapper;
import com.asset.management.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门服务：多级部门树管理 + 分页搜索 + CSV 批量导入导出
 */
@Service
public class DeptService {

    @Resource
    private SysDeptMapper deptMapper;
    @Resource
    private SysUserMapper userMapper;

    // ==================== 查询 ====================

    /**
     * 完整部门树
     */
    public List<DeptDTO> listTree() {
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));
        return buildTree(all, Constants.ROOT_PARENT_ID);
    }

    /**
     * 分页 + 搜索（扁平列表，附父部门名称）
     */
    public PageResult<DeptDTO> page(long pageNum, long pageSize, String deptName, String leader, String phone, Integer status) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<SysDept>()
                .like(deptName != null && !deptName.isBlank(), SysDept::getDeptName, deptName)
                .like(leader != null && !leader.isBlank(), SysDept::getLeader, leader)
                .like(phone != null && !phone.isBlank(), SysDept::getPhone, phone)
                .eq(status != null, SysDept::getStatus, status)
                .orderByAsc(SysDept::getSort)
                .orderByAsc(SysDept::getId);
        Page<SysDept> page = deptMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        Map<Long, String> nameMap = new HashMap<>();
        for (SysDept d : deptMapper.selectList(null)) {
            nameMap.put(d.getId(), d.getDeptName());
        }
        List<DeptDTO> records = new ArrayList<>();
        for (SysDept d : page.getRecords()) {
            DeptDTO dto = toDTO(d);
            dto.setParentName(Constants.ROOT_PARENT_ID.equals(d.getParentId())
                    ? null : nameMap.getOrDefault(d.getParentId(), "未知上级"));
            records.add(dto);
        }
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    // ==================== 新增 / 修改 / 删除 ====================

    @Transactional(rollbackFor = Exception.class)
    public void add(DeptDTO dto) {
        checkNameUnique(dto.getDeptName(), dto.getParentId(), null);
        SysDept dept = new SysDept();
        applyDTO(dept, dto);
        deptMapper.insert(dept);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(DeptDTO dto) {
        SysDept dept = deptMapper.selectById(dto.getId());
        if (dept == null) {
            throw new BusinessException(ResultCode.DEPT_NOT_FOUND);
        }
        Long newParentId = dto.getParentId() == null ? Constants.ROOT_PARENT_ID : dto.getParentId();
        if (dto.getId().equals(newParentId)) {
            throw new BusinessException(ResultCode.DEPT_CYCLE);
        }
        // 不能移动到自身子孙部门下
        if (!Constants.ROOT_PARENT_ID.equals(newParentId)) {
            List<Long> descendantIds = new ArrayList<>();
            collectDescendantIds(deptMapper.selectList(null), dto.getId(), descendantIds);
            if (descendantIds.contains(newParentId)) {
                throw new BusinessException(ResultCode.DEPT_CYCLE);
            }
        }
        checkNameUnique(dto.getDeptName(), newParentId, dto.getId());
        applyDTO(dept, dto);
        dept.setParentId(newParentId);
        deptMapper.updateById(dept);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResultCode.DEPT_NOT_FOUND);
        }
        Long childCount = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ResultCode.DEPT_HAS_CHILDREN);
        }
        // 用户表 department 存部门名称，按名称检查是否仍有用户挂在该部门
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDepartment, dept.getDeptName()));
        if (userCount != null && userCount > 0) {
            throw new BusinessException(ResultCode.DEPT_HAS_USERS);
        }
        deptMapper.deleteById(id);
    }

    // ==================== CSV 导出 ====================

    /**
     * 导出全部部门为 CSV（UTF-8 BOM，Excel 可直接打开）
     */
    public String exportCsv() {
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort).orderByAsc(SysDept::getId));
        Map<Long, String> nameMap = new HashMap<>();
        for (SysDept d : all) {
            nameMap.put(d.getId(), d.getDeptName());
        }
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // UTF-8 BOM，避免 Excel 中文乱码
        sb.append("部门名称,上级部门,负责人,联系电话,邮箱,排序,状态,备注\r\n");
        for (SysDept d : all) {
            String parentName = Constants.ROOT_PARENT_ID.equals(d.getParentId())
                    ? "" : nameMap.getOrDefault(d.getParentId(), "");
            sb.append(csvEscape(d.getDeptName())).append(',')
                    .append(csvEscape(parentName)).append(',')
                    .append(csvEscape(d.getLeader())).append(',')
                    .append(csvEscape(d.getPhone())).append(',')
                    .append(csvEscape(d.getEmail())).append(',')
                    .append(d.getSort() == null ? 0 : d.getSort()).append(',')
                    .append(d.getStatus() != null && d.getStatus() == CommonStatus.ENABLED ? "启用" : "禁用").append(',')
                    .append(csvEscape(d.getRemark())).append("\r\n");
        }
        return sb.toString();
    }

    // ==================== CSV 导入 ====================

    /**
     * 批量导入部门 CSV。
     * 表头：部门名称,上级部门,负责人,联系电话,邮箱,排序,状态,备注
     * 上级部门按名称匹配，为空表示顶级；匹配失败该行失败。
     *
     * @return 结果描述信息
     */
    @Transactional(rollbackFor = Exception.class)
    public String importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.DEPT_IMPORT_FILE_EMPTY);
        }
        List<SysDept> existing = deptMapper.selectList(null);
        Map<String, Long> nameToId = new HashMap<>();
        for (SysDept d : existing) {
            nameToId.put(d.getDeptName(), d.getId());
        }

        int success = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String cleaned = line.replace("\uFEFF", "").trim();
                if (cleaned.isEmpty() || lineNo == 1 && cleaned.contains("部门名称")) {
                    continue; // 跳过空行与表头
                }
                String[] cols = parseCsvLine(cleaned);
                if (cols.length < 1 || cols[0].isBlank()) {
                    errors.add("第 " + lineNo + " 行：部门名称为空");
                    continue;
                }
                try {
                    String deptName = cols[0].trim();
                    if (nameToId.containsKey(deptName)) {
                        errors.add("第 " + lineNo + " 行：部门「" + deptName + "」已存在，跳过");
                        continue;
                    }
                    SysDept dept = new SysDept();
                    dept.setDeptName(deptName);
                    // 上级部门按名称匹配
                    Long parentId = Constants.ROOT_PARENT_ID;
                    if (cols.length > 1 && !cols[1].isBlank()) {
                        Long matched = nameToId.get(cols[1].trim());
                        if (matched == null) {
                            errors.add("第 " + lineNo + " 行：上级部门「" + cols[1].trim() + "」不存在");
                            continue;
                        }
                        parentId = matched;
                    }
                    dept.setParentId(parentId);
                    if (cols.length > 2) dept.setLeader(cols[2].trim());
                    if (cols.length > 3) dept.setPhone(cols[3].trim());
                    if (cols.length > 4) dept.setEmail(cols[4].trim());
                    if (cols.length > 5 && !cols[5].isBlank()) {
                        try {
                            dept.setSort(Integer.parseInt(cols[5].trim()));
                        } catch (NumberFormatException e) {
                            dept.setSort(0);
                        }
                    }
                    if (cols.length > 6) {
                        dept.setStatus("禁用".equalsIgnoreCase(cols[6].trim()) ? CommonStatus.DISABLED : CommonStatus.ENABLED);
                    } else {
                        dept.setStatus(CommonStatus.ENABLED);
                    }
                    if (cols.length > 7) dept.setRemark(cols[7].trim());
                    deptMapper.insert(dept);
                    nameToId.put(deptName, dept.getId()); // 支持文件内先父后子
                    success++;
                } catch (Exception e) {
                    errors.add("第 " + lineNo + " 行：" + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BusinessException(500, "读取导入文件失败：" + e.getMessage());
        }
        StringBuilder result = new StringBuilder("导入完成：成功 " + success + " 条");
        if (!errors.isEmpty()) {
            result.append("，失败/跳过 ").append(errors.size()).append(" 条：");
            result.append(String.join("；", errors));
        }
        return result.toString();
    }

    // ==================== 私有工具 ====================

    private void applyDTO(SysDept dept, DeptDTO dto) {
        dept.setParentId(dto.getParentId() == null ? Constants.ROOT_PARENT_ID : dto.getParentId());
        dept.setDeptName(dto.getDeptName());
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setEmail(dto.getEmail());
        dept.setSort(dto.getSort() == null ? 0 : dto.getSort());
        dept.setStatus(dto.getStatus() == null ? CommonStatus.ENABLED : dto.getStatus());
        dept.setRemark(dto.getRemark());
    }

    private List<DeptDTO> buildTree(List<SysDept> all, Long parentId) {
        List<DeptDTO> result = new ArrayList<>();
        for (SysDept dept : all) {
            if (parentId.equals(dept.getParentId())) {
                DeptDTO dto = toDTO(dept);
                dto.setChildren(buildTree(all, dept.getId()));
                result.add(dto);
            }
        }
        return result;
    }

    private DeptDTO toDTO(SysDept dept) {
        DeptDTO dto = new DeptDTO();
        dto.setId(dept.getId());
        dto.setParentId(dept.getParentId());
        dto.setDeptName(dept.getDeptName());
        dto.setLeader(dept.getLeader());
        dto.setPhone(dept.getPhone());
        dto.setEmail(dept.getEmail());
        dto.setSort(dept.getSort());
        dto.setStatus(dept.getStatus());
        dto.setRemark(dept.getRemark());
        dto.setCreateTime(dept.getCreateTime());
        return dto;
    }

    /** 同级下部门名称唯一 */
    private void checkNameUnique(String deptName, Long parentId, Long excludeId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptName, deptName)
                .eq(SysDept::getParentId, parentId == null ? Constants.ROOT_PARENT_ID : parentId);
        if (excludeId != null) {
            wrapper.ne(SysDept::getId, excludeId);
        }
        if (deptMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DEPT_NAME_EXISTS);
        }
    }

    private void collectDescendantIds(List<SysDept> all, Long parentId, List<Long> result) {
        for (SysDept d : all) {
            if (parentId.equals(d.getParentId())) {
                result.add(d.getId());
                collectDescendantIds(all, d.getId(), result);
            }
        }
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** 简单 CSV 行解析：支持双引号包裹与 "" 转义 */
    private String[] parseCsvLine(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                cols.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cols.add(cur.toString());
        return cols.toArray(new String[0]);
    }
}
