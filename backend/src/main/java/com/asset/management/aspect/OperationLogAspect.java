package com.asset.management.aspect;

import com.asset.management.common.annotation.OperationLog;
import com.asset.management.security.SecurityUtil;
import com.asset.management.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 操作日志切面：拦截标注 @OperationLog 的 Controller 方法，异步记录关键操作
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Resource
    private OperationLogService operationLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        com.asset.management.entity.OperationLog entity = new com.asset.management.entity.OperationLog();
        entity.setModule(operationLog.module());
        entity.setOperationType(operationLog.type());
        entity.setMethod(pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            entity.setRequestMethod(request.getMethod());
            entity.setRequestUrl(request.getRequestURI());
            entity.setIp(getClientIp(request));
            entity.setParams(truncate(serializeParams(pjp.getArgs())));
        }

        try {
            entity.setUserId(SecurityUtil.getUserId());
            entity.setUsername(SecurityUtil.getUsername());
        } catch (Exception e) {
            entity.setUsername("anonymous");
        }

        Object result;
        try {
            result = pjp.proceed();
            entity.setStatus(1);
        } catch (Throwable t) {
            entity.setStatus(0);
            entity.setErrorMsg(truncate(t.getMessage()));
            throw t;
        } finally {
            entity.setDuration(System.currentTimeMillis() - start);
            entity.setCreateTime(LocalDateTime.now());
            operationLogService.record(entity);
        }
        return result;
    }

    private String serializeParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        try {
            Object[] filtered = java.util.Arrays.stream(args)
                    .filter(arg -> !(arg instanceof HttpServletRequest)
                            && !(arg instanceof HttpServletResponse)
                            && !(arg instanceof MultipartFile))
                    .toArray();
            if (filtered.length == 0) {
                return "";
            }
            return objectMapper.writeValueAsString(filtered);
        } catch (Exception e) {
            return "";
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 2000 ? text.substring(0, 2000) : text;
    }
}
