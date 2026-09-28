package com.studyroom.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class PageRequest {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 10;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long offset() {
        return (long) (page - 1) * size;
    }
}
