package com.example.test.common;

import java.util.List;

/**
 * 分页响应结构，与 Express 侧 page(res, list, total, pageNum, pageSize) 保持一致：
 * { list, total, pageNum, pageSize }
 */
public class PageResult<T> {

    private List<T> list;
    private long total;
    private long pageNum;
    private long pageSize;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, long pageNum, long pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPageNum() {
        return pageNum;
    }

    public void setPageNum(long pageNum) {
        this.pageNum = pageNum;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }
}
