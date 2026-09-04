package com.example.test.common;

/**
 * 分页参数，与 Express 侧 utils/data.js 的 pagination 保持一致：
 * pageNum 默认 1，pageSize 默认 10、上限 100
 */
public class PageQuery {

    private final int pageNum;
    private final int pageSize;

    private PageQuery(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public static PageQuery of(Integer pageNum, Integer pageSize) {
        int num = pageNum == null ? 1 : Math.max(1, pageNum);
        int size = pageSize == null ? 10 : Math.min(100, Math.max(1, pageSize));
        return new PageQuery(num, size);
    }

    public int getPageNum() {
        return pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
