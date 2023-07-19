package com.volcano.range.dto;

public interface IRangeData {
    /**
     * 本方法最为核心，每个数据收集得都不一样，通过子类来实现，在mvc拦截器中会自动调用
     */
    void fillRangeData();

    /**
     * 释放资源
     */
    void remove();
}
