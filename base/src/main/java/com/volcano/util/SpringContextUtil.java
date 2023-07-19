package com.volcano.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 获取Bean及环境配置
 */
@Component
public class SpringContextUtil implements ApplicationContextAware{

    private static ApplicationContext context=null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException{
        context=applicationContext;
    }
    /**
     * 获取Bean
     * @param beanName
     * @param <T>
     * @return
     */
    public static <T> T getBean(String beanName){
        return (T) context.getBean(beanName);
    }


    /**
     * 获取Bean
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class clazz){
        return (T) context.getBean(clazz);
    }
    /**
     * 获取当前环境
     * @return
     */
    public static String getActiveProfile(){
        return context.getEnvironment().getActiveProfiles()[0];
    }
    //?
    public static String getMessage(String key){
        return context.getMessage(key, null, Locale.getDefault());
    }
}