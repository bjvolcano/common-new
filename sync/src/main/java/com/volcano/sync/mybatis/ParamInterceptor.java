package com.volcano.sync.mybatis;

import com.volcano.mybatis.BaseInterceptor;
import com.volcano.sync.service.ISyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.springframework.transaction.TransactionStatus;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;


@Intercepts({
        @Signature(type = ParameterHandler.class, method = "setParameters", args = PreparedStatement.class)
})
@Slf4j
//@Component
public class ParamInterceptor extends BaseInterceptor implements Interceptor {
    @Resource
    private ISyncService syncService;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            Object target = invocation.getTarget();
            // 获取拦截器拦截的设置参数对象DefaultParameterHandler
            DefaultParameterHandler parameterHandler = (DefaultParameterHandler) target;
            // 通过mybatis的反射来获取对应的值
            //MetaObject metaResultSetHandler = MetaObject.forObject(parameterHandler, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY, REFLECTOR_FACTORY);
            //MappedStatement mappedStatement = (MappedStatement) metaResultSetHandler.getValue("mappedStatement");
            Class<?> clazz = DefaultParameterHandler.class;
            Field field = clazz.getDeclaredField("mappedStatement");
            field.setAccessible(true);
            MappedStatement mappedStatement = (MappedStatement) field.get(parameterHandler);

            Field parameterField = clazz.getDeclaredField("parameterObject");
            parameterField.setAccessible(true);
            Object parameter = parameterField.get(parameterHandler);
            //sql类型
            SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
            if (sqlCommandType.equals(SqlCommandType.INSERT) || sqlCommandType.equals(SqlCommandType.DELETE) || sqlCommandType.equals(SqlCommandType.UPDATE)) {
                //SpringManagedTransaction transaction = (SpringManagedTransaction) parameterHandler.getTransaction();
                //Connection connection = transaction.getConnection();

                BoundSql boundSql = mappedStatement.getBoundSql(parameter);
                String format = this.getSql4Oracle(mappedStatement.getConfiguration(), boundSql);
                log.info("execute sql : {}", format);
                TransactionStatus status = syncService.getTransactionStatus();
                syncService.buildSqls(status, format);
            }
        } catch (Exception e) {
            log.error("异常", e);
        }

        return invocation.proceed();
    }

}