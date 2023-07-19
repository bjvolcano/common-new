package com.volcano.cache.mybatis;

import com.volcano.cache.service.ICacheService;
import com.volcano.mybatis.BaseInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Intercepts({@Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}
)})
public class UpdateCacheInterceptor extends BaseInterceptor {
    private static final Logger log = LoggerFactory.getLogger(UpdateCacheInterceptor.class);
    @Autowired
    private ICacheService cacheService;

    public UpdateCacheInterceptor(ICacheService cacheService, Integer sort) {
        this.cacheService = cacheService;
        this.sort = sort;
    }

    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object args = invocation.getArgs()[1];
        BoundSql boundSql = mappedStatement.getBoundSql(args);
        String format = this.getSql(mappedStatement.getConfiguration(), boundSql);
        this.cacheService.getKey(format, null);
        this.cacheService.remove();
        Object returnValue = invocation.proceed();
        return returnValue;
    }
}
