package com.volcano.cache.mybatis;

import com.alibaba.druid.sql.SQLUtils;
import com.baomidou.mybatisplus.core.MybatisDefaultParameterHandler;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.core.parser.SqlInfo;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.toolkit.SqlParserUtils;
import com.volcano.cache.service.ICacheService;
import com.volcano.mybatis.MybatisParamsUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

@Intercepts({@Signature(
   type = StatementHandler.class,
   method = "prepare",
   args = {Connection.class, Integer.class}
)})
public class PaginationCacheInterceptor extends PaginationInterceptor {
   @Autowired
   ICacheService cacheService;

   public PaginationCacheInterceptor(ICacheService cacheService) {
      this.cacheService = cacheService;
   }

   @Override
   protected void queryTotal(boolean overflowCurrent, String sql, MappedStatement mappedStatement, BoundSql boundSql, IPage page, Connection connection) {
      try {
         PreparedStatement statement = connection.prepareStatement(sql);

         try {
            DefaultParameterHandler parameterHandler = new MybatisDefaultParameterHandler(mappedStatement, boundSql.getParameterObject(), boundSql);
            parameterHandler.setParameters(statement);
            long total = 0L;
            ResultSet resultSet = statement.executeQuery();

            try {
               if (resultSet.next()) {
                  total = resultSet.getLong(1);
               }
            } catch (Throwable var16) {
               if (resultSet != null) {
                  try {
                     resultSet.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }
               }

               throw var16;
            }

            if (resultSet != null) {
               resultSet.close();
            }

            page.setTotal(total);
            long pages = page.getPages();
            if (overflowCurrent && page.getCurrent() > pages) {
               page.setCurrent(1L);
            }

            this.saveTotal2Cache(mappedStatement, boundSql, page);
         } catch (Throwable var17) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable var14) {
                  var17.addSuppressed(var14);
               }
            }

            throw var17;
         }

         if (statement != null) {
            statement.close();
         }

      } catch (Exception var18) {
         throw ExceptionUtils.mpe("Error: Method queryTotal execution error of sql : \n %s \n", var18, new Object[]{sql});
      }
   }

   private void saveTotal2Cache(MappedStatement mappedStatement, BoundSql boundSql, IPage page) {
      Object parameterObject = boundSql.getParameterObject();
      List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
      String sql = SQLUtils.formatMySql(boundSql.getSql());
      if (!CollectionUtils.isEmpty(parameterMappings)) {
         TypeHandlerRegistry typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();

         Object value;
         for(Iterator var8 = parameterMappings.iterator(); var8.hasNext(); sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(MybatisParamsUtil.getParameterValue(value)))) {
            ParameterMapping parameterMapping = (ParameterMapping)var8.next();
            String propertyName = parameterMapping.getProperty();
            if (boundSql.hasAdditionalParameter(propertyName)) {
               value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
               value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
               value = parameterObject;
            } else {
               MetaObject metaObject = mappedStatement.getConfiguration().newMetaObject(parameterObject);
               value = metaObject.getValue(propertyName);
            }
         }
      }

      SqlInfo sqlInfo = SqlParserUtils.getOptimizeCountSql(page.optimizeCountSql(), (ISqlParser)null, sql);
      String topKey = this.cacheService.getKey();
      this.cacheService.getKey(sqlInfo.getSql(), (Object)null);
      ICacheService var10000 = this.cacheService;
      Long var10001 = page.getTotal();
      ICacheService var10002 = this.cacheService;
      var10000.put(var10001, ICacheService.EXPIRE, TimeUnit.SECONDS);
      this.cacheService.setKey(topKey);
   }
}
