package com.volcano.range.mapping;

import lombok.Data;

/**
 * @author volcano
 * @version 1.0
 * @date 2020/9/22 13:59
 */
@Data
public class SourceFromInfo {

    private boolean needAddCondition;

    private String alias;

    private boolean subQuery;

    private String tableName;
}
