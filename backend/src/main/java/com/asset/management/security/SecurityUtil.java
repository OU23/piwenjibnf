package com.asset.management.security;

import com.asset.management.common.exception.BusinessException;
import com.asset.management.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    /**
     * 获取当前登录用户 ID
     */
    public static Long getUserId() {
        return getLoginUser().getUser().getId();
    }

    /**
     * 获取当前登录用户名
     */
    public static String getUsername() {
        return getLoginUser().getUser().getUsername();
    }
}
