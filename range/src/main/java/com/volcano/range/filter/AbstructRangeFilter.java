package com.volcano.range.filter;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.volcano.range.mapping.ITableMapping;
import com.volcano.range.mapping.Mapping;
import com.volcano.range.mapping.SourceFromInfo;

/**
 * @author volcano
 * @version 1.0
 * @date 2020/9/23 16:51
 */
@Data
public abstract class AbstructRangeFilter implements IRangeFilter {
    protected ITableMapping mapping;
    protected ITableMapping exclude;

    public SQLExpr getFilterCondition(SourceFromInfo tableNameInfo) {
        if (excludeHasTable(tableNameInfo)) {
            return null;
        }

        Mapping map = getMapping(tableNameInfo);
        if (map != null) {
            String filterWhereSql = getFilterWhereSql(map);
            if (StringUtils.isEmpty(filterWhereSql)) {
                return null;
            }

            SQLExpr sqlExpr = SQLUtils.toMySqlExpr(filterWhereSql);
            return sqlExpr;
        }

        return null;
    }

    /**
     * 获取实际过滤where 条件
     *
     * @return
     */
    public abstract String getFilterWhereSql(Mapping tableNameInfo);

    /**
     * 检查排除映射
     *
     * @param tableNameInfo
     * @return
     */
    private boolean excludeHasTable(SourceFromInfo tableNameInfo) {
        if (exclude == null || CollectionUtils.isEmpty(exclude.getMappings()))
            return false;
        for (Mapping mapping : exclude.getMappings()) {
            if (mapping.getTable().equals(tableNameInfo.getTableName()))
                return true;
        }
        return false;
    }

    private Mapping getMapping(SourceFromInfo tableNameInfo) {
        if (mapping == null || CollectionUtils.isEmpty(mapping.getMappings()))
            return null;
        for (Mapping map : mapping.getMappings()) {
            if (map.getTable().equals(tableNameInfo.getTableName())) {
                return new Mapping(map.getTable(), map.getField(), tableNameInfo.getAlias());
            }
        }
        return null;
    }

    /**
     * 检查过滤映射
     *
     * @param tableNameInfo
     * @return
     */
    private boolean mappingHasTable(SourceFromInfo tableNameInfo) {
        if (mapping == null || CollectionUtils.isEmpty(mapping.getMappings()))
            return false;
        for (Mapping mapping : mapping.getMappings()) {
            if (mapping.getTable().equals(tableNameInfo.getTableName()))
                return true;
        }
        return false;
    }
}
