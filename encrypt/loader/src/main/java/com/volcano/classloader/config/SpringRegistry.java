package com.volcano.classloader.config;

import com.volcano.classloader.loader.EncryptClassLoader;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @Author bjvolcano
 * @Date 2021/5/11 1:39 下午
 * @Version 1.0
 */
@Component
@Data
@Slf4j
public class SpringRegistry implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        EncryptClassLoader instance = EncryptClassLoader.getInstance();
        if (instance == null) {
            postProcessBeanFactory(beanFactory);
            return;
        }

        log.info("Fill beanFactory to encrypt classloader and registry beans");
        instance.setBeanFactory((DefaultListableBeanFactory) beanFactory);
        if (!(beanFactory.getBeanClassLoader() instanceof EncryptClassLoader)) {
            beanFactory.setBeanClassLoader(instance);
        }

        instance.registryBeans();
    }
}
