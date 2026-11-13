package com.asset.management.controller;

import com.asset.management.common.annotation.OperationLog;
import com.asset.management.common.constant.Constants;
import com.asset.management.common.result.Result;
import com.asset.management.dto.LoginDTO;
import com.asset.management.dto.LoginVO;
import com.asset.management.dto.PasswordDTO;
import com.asset.management.security.SecurityUtil;
import com.asset.management.service.AuthService;
import com.asset.management.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;
    @Resource
    private UserService userService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @GetMapping("/info")
    public Result<LoginVO> info() {
        return Result.success(authService.getInfo());
    }

    @OperationLog(module = "认证管理", type = Constants.OPERATION_UPDATE)
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordDTO dto) {
        userService.changePassword(SecurityUtil.getUserId(), dto);
        return Result.success();
    }
}
