package com.volcano.test.config.range;

import com.volcano.range.dto.AbstractRangeData;

/**
 * 用于收集、传输过滤sql得数据
 * 比如  where xxx in (1,2,3)//这类得数据
 */
public class UserRangeRangeData extends AbstractRangeData {


    public Long getUserId() {
        return get();
    }

    @Override
    public void fillRangeData() {

    }
}
