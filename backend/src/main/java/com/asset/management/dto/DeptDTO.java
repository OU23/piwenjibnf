package com.asset.management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门 DTO（树形结构 + 新增/修改入参）
 */
@Data
public class DeptDTO {

    private Long id;

    /** 父部门 ID，顶级为 0 */
    private Long parentId;

    /** 部门名称 */
    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 排序 */
    private Integer sort;

    /** 状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 父部门名称（分页列表展示用） */
    private String parentName;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 子部门 */
    private List<DeptDTO> children;
}
