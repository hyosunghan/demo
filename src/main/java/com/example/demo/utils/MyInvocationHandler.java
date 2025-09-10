package com.example.demo.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

public class MyInvocationHandler implements InvocationHandler {

    private Object target;

    private Consumer<Method> before;

    private Consumer<Method> after;

    public MyInvocationHandler(Object target, Consumer<Method> before, Consumer<Method> after) {
        this.target = target;
        this.before = before;
        this.after = after;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        before.accept(method);
        Object result = method.invoke(target, args);
        after.accept(method);
        return result;
    }

    public static Object getProxy(Object o, Consumer<Method> before, Consumer<Method> after) {
        Class<?> clazz = o.getClass();
        return Proxy.newProxyInstance(clazz.getClassLoader(),
                clazz.getInterfaces(),
                new MyInvocationHandler(o, before, after));
    }
}
