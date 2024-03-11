package com.volcano.sync.mybatis;

import com.volcano.mybatis.BaseInterceptor;
import com.volcano.sync.service.ISyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.mybatis.spring.transaction.SpringManagedTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.TransactionStatus;
import javax.annotation.Resource;
import java.sql.Connection;

@Intercepts({@Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}
)})
@Slf4j
public class UpdateCacheInterceptor extends BaseInterceptor {

    @Resource
    private ISyncService syncService;

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Executor executor = realTarget(invocation.getTarget());
        TransactionStatus status = syncService.getTransactionStatus();
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object args = invocation.getArgs()[1];
        BoundSql boundSql = mappedStatement.getBoundSql(args);
        String format = this.getSql4Oracle(mappedStatement.getConfiguration(), boundSql);
        log.info("execute sql : {}", format);
        syncService.buildSqls(status, format);
        Object returnValue = invocation.proceed();
        return returnValue;
    }

    @Override
    public void remove() {
    }
}
