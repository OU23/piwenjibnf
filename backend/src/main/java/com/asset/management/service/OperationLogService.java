package com.asset.management.service;

import com.asset.management.common.result.PageResult;
import com.asset.management.entity.OperationLog;
import com.asset.management.mapper.OperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志服务：异步记录 + 分页查询
 */
@Slf4j
@Service
public class OperationLogService {

    @Resource
    private OperationLogMapper operationLogMapper;

    /**
     * 异步保存操作日志（不阻塞业务响应）
     */
    @Async
    public void record(OperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("操作日志记录失败", e);
        }
    }

    public PageResult<OperationLog> page(long pageNum, long pageSize, String username, String module) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .like(StringUtils.hasText(username), OperationLog::getUsername, username)
                .like(StringUtils.hasText(module), OperationLog::getModule, module)
                .orderByDesc(OperationLog::getId);
        Page<OperationLog> page = operationLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }
}
