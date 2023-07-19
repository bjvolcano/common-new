package com.volcano.range.filter;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.volcano.range.mapping.SourceFromInfo;

/**
 * @author volcano
 * @version 1.0
 * @date 2020/9/22 14:15
 */
public interface IRangeFilter {

    /**
     * 根据表信息拼接过滤 条件
     *
     * @param tableNameInfo 表信息
     * @return 拼接后的条件
     */
     SQLExpr getFilterCondition(SourceFromInfo tableNameInfo);

}
