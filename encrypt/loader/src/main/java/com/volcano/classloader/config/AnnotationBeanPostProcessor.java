package com.volcano.classloader.config;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class AnnotationBeanPostProcessor extends ScheduledAnnotationBeanPostProcessor {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        super.onApplicationEvent(event);
        System.out.println(event);
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        super.postProcessMergedBeanDefinition(beanDefinition, beanType, beanName);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return super.postProcessBeforeInitialization(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return super.postProcessAfterInitialization(bean, beanName);
    }

    @Override
    protected void processScheduled(Scheduled scheduled, Method method, Object bean) {
        super.processScheduled(scheduled, method, bean);
    }
}

