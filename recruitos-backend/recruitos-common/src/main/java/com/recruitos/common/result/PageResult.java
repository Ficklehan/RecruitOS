package com.recruitos.common.result;

import java.io.Serializable;
import java.util.List;

/**
 * Pagination result wrapper
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private long total;
    private List<T> list;
    private int pageNum;
    private int pageSize;

    public PageResult() {
    }

    public PageResult(long total, List<T> list, int pageNum, int pageSize) {
        this.total = total;
        this.list = list;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    // Getters and Setters
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
