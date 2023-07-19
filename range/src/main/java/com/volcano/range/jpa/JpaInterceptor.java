package com.volcano.range.jpa;

import com.volcano.util.SpringContextUtil;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import com.volcano.range.filter.MysqlFilter;

@Lazy
public class JpaInterceptor implements StatementInspector {

    @Autowired
    private MysqlFilter mysqlFilter = SpringContextUtil.getBean(MysqlFilter.class);

    @Override
    public String inspect(String sql) {
        //sql = sql.toLowerCase().replaceAll("[\\s]+", " ");
        String filterSql = mysqlFilter.filter(sql);
        return filterSql;
    }
}