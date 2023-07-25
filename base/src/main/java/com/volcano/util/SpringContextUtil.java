package com.volcano.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Modifier;
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

    public static BeanDefinition buildBeanDefinition(Class cla) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(cla);
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
        //设置当前bean定义对象是单利的
        Scope scope = (Scope) cla.getAnnotation(Scope.class);
        String scopeString = ConfigurableBeanFactory.SCOPE_SINGLETON;
        if (scope != null) {
            scopeString = scope.value();
        }

        beanDefinition.setScope(scopeString);
        Lazy lazy = (Lazy) cla.getAnnotation(Lazy.class);
        if (lazy != null) {
            beanDefinition.setLazyInit(true);
        }

        return beanDefinition;
    }

    /**
     * 方法描述 判断class对象是否带有spring的注解
     *
     * @param cla jar中的每一个class
     * @return true 是spring bean   false 不是spring bean
     * @method isSpringBeanClass
     */
    public static boolean isSpringBeanClass(Class<?> cla) {
        if (cla == null) {
            return false;
        }
        //是否是接口
        if (cla.isInterface()) {
            return false;
        }

        //是否是抽象类
        if (Modifier.isAbstract(cla.getModifiers())) {
            return false;
        }

        if (cla.getAnnotation(Component.class) != null) {
            return true;
        }

        if (cla.getAnnotation(Repository.class) != null) {
            return true;
        }

        if (cla.getAnnotation(Service.class) != null) {
            return true;
        }

        return false;
    }
}