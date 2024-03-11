package com.volcano.sync.autoconfigretion;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.volcano.sync.mvc.SyncInterceptor;
import com.volcano.sync.mybatis.ParamInterceptor;
import com.volcano.mybatis.BaseInterceptor;
import com.volcano.util.SpringContextUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.InterceptorChain;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@ComponentScan({"com.volcano.sync", "com.volcano.util"})
@Configuration
@Slf4j
public class SyncAutoConfig implements WebMvcConfigurer, ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    SpringContextUtil springContextUtil;

    /*@Bean
    public UpdateCacheInterceptor updateCacheInterceptor() {
        UpdateCacheInterceptor updateCacheInterceptor = new UpdateCacheInterceptor();
        updateCacheInterceptor.setSort(3);
        return updateCacheInterceptor;
    }*/

    @Bean
    public ParamInterceptor paraminterceptor() {
        ParamInterceptor paramInterceptor = new ParamInterceptor();
        paramInterceptor.setSort(4);
        return paramInterceptor;
    }

    @Bean
    @Lazy
    @Order(999)
    public SyncInterceptor syncInterceptor() {
        SyncInterceptor syncInterceptor = new SyncInterceptor();
        //syncInterceptor set something
        return syncInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.syncInterceptor());
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {
        try {
            SqlSessionFactory sqlSessionFactory = SpringContextUtil.getBean(SqlSessionFactory.class);
            org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
            List<Interceptor> interceptors = configuration.getInterceptors();
            if (!CollectionUtils.isEmpty(interceptors)) {
                InterceptorChain interceptorChain = new InterceptorChain();
                List<Interceptor> otherInterceptors = interceptors.stream().filter((x) -> !(x instanceof PaginationInterceptor)).collect(Collectors.toList());
                List<Interceptor> baseInterceptors = otherInterceptors.stream().filter((x) -> x instanceof BaseInterceptor).sorted().collect(Collectors.toList());
                baseInterceptors.forEach((x) -> {
                    interceptorChain.addInterceptor(x);
                    log.info("Reconfig interceptor chain add :{}", x);
                });
                log.info("排序组件中得拦截器顺序--->ok");
                if (otherInterceptors.size() > baseInterceptors.size()) {
                    otherInterceptors.removeAll(baseInterceptors);
                    otherInterceptors.forEach((x) -> {
                        interceptorChain.addInterceptor(x);
                        log.info("Reconfig interceptor chain add :{}", x);
                    });
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
