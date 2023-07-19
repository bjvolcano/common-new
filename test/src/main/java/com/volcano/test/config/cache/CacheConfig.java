package com.volcano.test.config.cache;

import com.volcano.cache.mvc.CacheInterceptor;
import com.volcano.conf.BaseConfig;
import com.volcano.range.mvc.RangeMVCInterceptor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Field;
import java.util.List;

@Import(com.volcano.redission.RedissonConfig.class)
@Component
@Slf4j
public class CacheConfig extends BaseConfig implements WebMvcConfigurer {
    /**
     * 设置cache拦截器 排除地址
     *
     * @param registry
     */
    @SneakyThrows
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        Field registrations = registry.getClass().getDeclaredField("registrations");
        registrations.setAccessible(true);
        List<InterceptorRegistration> interceptors = (List<InterceptorRegistration>) registrations.get(registry);

        if (!CollectionUtils.isEmpty(interceptors)) {
            for (InterceptorRegistration in : interceptors) {
                HandlerInterceptor interceptor = getInterceptor(in);
                if (interceptor instanceof CacheInterceptor) {
                    in.excludePathPatterns("/sys/login", "/sys/code/*");
                    log.info("设置cache拦截器 排除地址--->ok");
                }
            }
        }
    }
}
