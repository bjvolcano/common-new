package com.volcano.test.config.range;

import com.volcano.range.filter.AbstructRangeFilter;
import com.volcano.range.mapping.Mapping;
import org.springframework.util.StringUtils;

/**
 * @author volcano
 * @version 1.0
 * @date 2020/9/23 17:46
 */

public class AdminRangeFilter extends AbstructRangeFilter {
    private UserRangeRangeData rangeData;

    public AdminRangeFilter(UserRangeRangeData rangeData) {
        this.rangeData = rangeData;
    }

    @Override
    public String getFilterWhereSql(Mapping mapping) {
        Long userId = rangeData.getUserId();
        if (userId == null) {
            return null;
        }
        StringBuilder sql = new StringBuilder(" ");
        if (!StringUtils.isEmpty(mapping.getAlias())) {
            sql.append(mapping.getAlias());
        }else {
            sql.append(mapping.getTable());
        }
        sql.append(".").append(mapping.getField()).append(" <> 1 ");
        return sql.toString();
    }

}
