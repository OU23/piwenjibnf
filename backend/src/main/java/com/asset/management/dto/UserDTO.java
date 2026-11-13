package com.asset.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 用户新增/修改参数
 */
@Data
public class UserDTO {

    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码（新增必填，修改可留空） */
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private String department;

    private Integer status;

    private String remark;

    /** 分配的角色 ID 列表 */
    private List<Long> roleIds;
}
