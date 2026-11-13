package com.asset.management.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果封装
 */
@Data
public class PageResult<T> implements Serializable {

    /** 当前页码 */
    private long pageNum;
    /** 每页条数 */
    private long pageSize;
    /** 总条数 */
    private long total;
    /** 总页数 */
    private long pages;
    /** 数据列表 */
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long pageNum, long pageSize, long total, List<T> records) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize;
        this.records = records;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }
}
