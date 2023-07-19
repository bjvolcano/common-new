package com.volcano.conf;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;

import java.lang.reflect.Field;

public class BaseConfig {

    protected HandlerInterceptor getInterceptor(InterceptorRegistration interceptorRegistration) throws NoSuchFieldException, IllegalAccessException {
        Field interceptorField = interceptorRegistration.getClass().getDeclaredField("interceptor");
        interceptorField.setAccessible(true);
        return  (HandlerInterceptor) interceptorField.get(interceptorRegistration);
    }
}
