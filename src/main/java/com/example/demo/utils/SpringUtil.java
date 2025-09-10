package com.example.demo.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringUtil implements ApplicationContextAware {

    private static ApplicationContext context;

    private static DefaultListableBeanFactory defaultListableBeanFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
        defaultListableBeanFactory = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
    }

    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        return context.getBean(name, clazz);
    }

    public static void registerBean(String beanName, Object bean) {
        defaultListableBeanFactory.registerSingleton(beanName, bean);
    }

    public static void removeBean(String beanName) {
        defaultListableBeanFactory.destroySingleton(beanName);
    }

    public static void replaceBean(String beanName, Object bean) {
        removeBean(beanName);
        registerBean(beanName, bean);
    }
}
