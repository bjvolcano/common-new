package com.volcano.cache.mybatis;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.core.parser.SqlInfo;
import com.baomidou.mybatisplus.extension.toolkit.SqlParserUtils;
import com.volcano.cache.service.ICacheService;
import com.volcano.mybatis.BaseInterceptor;
import com.volcano.mybatis.MybatisParamsUtil;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

@Intercepts({@Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
)})
public class QueryCacheInterceptor extends BaseInterceptor {
    private static final Logger log = LoggerFactory.getLogger(QueryCacheInterceptor.class);
    private static final Random random = new Random(120L);
    @Autowired
    private ICacheService cacheService;

    public QueryCacheInterceptor(ICacheService cacheService, Integer sort) {
        this.cacheService = cacheService;
        this.sort = sort;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        return invocation.getTarget() instanceof Executor ? this.dealCacheProceed(invocation) : invocation.proceed();
    }

    private Object dealCacheProceed(Invocation invocation) throws InvocationTargetException, IllegalAccessException, SQLException {
        Executor executor = (Executor) realTarget(invocation.getTarget());
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object args = invocation.getArgs()[1];
        BoundSql boundSql = mappedStatement.getBoundSql(args);
        String format = this.getSql(mappedStatement.getConfiguration(), boundSql);
        Object result = this.cacheService.getBySqlKey(format, (Object) null, executor);
        if (result == null) {
            try {
                this.cacheService.hotKeyLock(this.cacheService.getKey());
                result = this.cacheService.get();
                if (result != null) {
                    log.debug("读取缓存成功！");
                    return result;
                }

                result = invocation.proceed();
                if (result != null) {
                    //log.info("缓存失效后再次读取到信息 {}", JSON.toJSONString(result));
                    this.cacheService.put(result, Math.abs(ICacheService.EXPIRE - (long) random.nextInt(120)), TimeUnit.SECONDS);
                }
            } finally {
                this.cacheService.unHotKeyLock(this.cacheService.getKey());
            }
        } else {
            log.debug("走缓存直接返回\nsql:{}\nkey:{},value:{}", new Object[]{format, this.cacheService.getKey(), JSON.toJSONString(result)});
        }

        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public String getSql(Configuration configuration, BoundSql boundSql) throws SQLException {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = SQLUtils.formatMySql(boundSql.getSql());
        if (!CollectionUtils.isEmpty(parameterMappings)) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();

            Object value;
            for (Iterator var7 = parameterMappings.iterator(); var7.hasNext(); sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(MybatisParamsUtil.getParameterValue(value)))) {
                ParameterMapping parameterMapping = (ParameterMapping) var7.next();
                String propertyName = parameterMapping.getProperty();
                if (boundSql.hasAdditionalParameter(propertyName)) {
                    value = boundSql.getAdditionalParameter(propertyName);
                } else if (parameterObject == null) {
                    value = null;
                } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                    value = parameterObject;
                } else {
                    MetaObject metaObject = configuration.newMetaObject(parameterObject);
                    value = metaObject.getValue(propertyName);
                }
            }
        }

        sql = this.dealPageSql(sql, parameterObject);
        return sql;
    }

    private String dealPageSql(String sql, Object parameterObject) {
        if (parameterObject == null) {
            return sql;
        } else {
            IPage<?> page = null;
            if (parameterObject instanceof IPage) {
                page = (IPage) parameterObject;
            } else if (parameterObject instanceof Map) {
                Iterator var4 = ((Map) parameterObject).values().iterator();

                while (var4.hasNext()) {
                    Object arg = var4.next();
                    if (arg instanceof IPage) {
                        page = (IPage) arg;
                    }
                }
            }

            if (page != null) {
                String topKey = this.cacheService.getKey();
                SqlInfo sqlInfo = SqlParserUtils.getOptimizeCountSql(page.optimizeCountSql(), (ISqlParser) null, sql);
                this.cacheService.getKey(sqlInfo.getSql(), (Object) null);
                Long total = (Long) this.cacheService.get();
                if (total != null) {
                    page.setTotal(total);
                }

                this.cacheService.setKey(topKey);
                sql = sql + " limit " + page.offset() + "," + page.getSize();
            }

            return sql;
        }
    }
}
