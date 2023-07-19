package com.volcano.cache.autoconfigretion;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.volcano.cache.mvc.CacheInterceptor;
import com.volcano.cache.mybatis.PaginationCacheInterceptor;
import com.volcano.cache.mybatis.QueryCacheInterceptor;
import com.volcano.cache.mybatis.UpdateCacheInterceptor;
import com.volcano.cache.service.CPCacheService;
import com.volcano.cache.service.ICacheService;
import com.volcano.mybatis.BaseInterceptor;
import com.volcano.util.SpringContextUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.InterceptorChain;
import org.apache.ibatis.session.SqlSessionFactory;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Order(999)
@Lazy
@ComponentScan({"com.volcano.cache", "com.volcano.util"})
@Configuration
public class CacheAutoConfig implements WebMvcConfigurer, ApplicationListener<ApplicationStartedEvent> {
    private static final Logger log = LoggerFactory.getLogger(CacheAutoConfig.class);
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    SpringContextUtil springContextUtil;
    @Autowired
    CacheConfig cacheConfig;
    @Autowired
    ICacheService cacheService;

    @Bean
    public CacheConfig initCacheConfig() {
        return new CacheConfig();
    }

    @ConditionalOnProperty(
            name = {"common.cache.mode"},
            matchIfMissing = true,
            havingValue = "cp"
    )
    @Bean
    public ICacheService initCPCacheService() {
        log.info("init cache by CP!");
        return new CPCacheService(this.redissonClient, this.cacheConfig);
    }

    @ConditionalOnProperty(
            name = {"common.cache.mode"},
            havingValue = "ap"
    )
    @Bean
    public ICacheService initAPCacheService() {
        log.info("init cache by AP!");
        return new CPCacheService(this.redissonClient, this.cacheConfig);
    }

    @Bean
    public QueryCacheInterceptor queryCacheInterceptor() {
        return new QueryCacheInterceptor(this.cacheService, -2);
    }

    @Bean
    public UpdateCacheInterceptor updateCacheInterceptor() {
        return new UpdateCacheInterceptor(this.cacheService, 3);
    }

    @Bean
    @Lazy
    @Order(999)
    public CacheInterceptor cacheInterceptor() {
        CacheInterceptor cacheInterceptor = new CacheInterceptor();
        cacheInterceptor.setCacheService(this.cacheService);
        return cacheInterceptor;
    }

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.cacheInterceptor());
    }

    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {
        try {
            SqlSessionFactory sqlSessionFactory = SpringContextUtil.getBean(SqlSessionFactory.class);
            org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
            List<Interceptor> interceptors = configuration.getInterceptors();
            if (!CollectionUtils.isEmpty(interceptors)) {
                InterceptorChain interceptorChain = new InterceptorChain();
                List<Interceptor> otherInterceptors = interceptors.stream().filter((x) -> !(x instanceof PaginationInterceptor)).collect(Collectors.toList());
                List<Interceptor> baseInterceptors = otherInterceptors.stream().filter((x) -> x instanceof BaseInterceptor).sorted().collect(Collectors.toList());
                baseInterceptors.forEach((x) -> interceptorChain.addInterceptor(x));
                log.info("排序组件中得拦截器顺序--->ok");
                if (otherInterceptors.size() > baseInterceptors.size()) {
                    otherInterceptors.removeAll(baseInterceptors);
                    otherInterceptors.forEach((x) -> {
                        interceptorChain.addInterceptor(x);
                    });
                }

                List<Interceptor> pageInterceptors = interceptors.stream().filter((x) ->
                        x instanceof PaginationInterceptor
                ).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(pageInterceptors)) {
                    interceptorChain.addInterceptor(new PaginationCacheInterceptor(this.cacheService));
                    log.info("容器中有自带myabtisplus 分页插件，替换成缓存组件中得分页插件--->ok");
                }

                Field field = org.apache.ibatis.session.Configuration.class.getDeclaredField("interceptorChain");
                field.setAccessible(true);
                field.set(configuration, interceptorChain);
                log.info("注入组装好得拦截器链--->ok");
            }

        } catch (Exception e) {
            log.error("初始化缓存组件异常", e);
        }
    }
}
