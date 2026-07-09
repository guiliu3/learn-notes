package com.learn.spring.aop.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * JDK动态代理核心：InvocationHandler
 *
 * 原理：
 *   - JVM 运行时生成 $Proxy0 字节码，继承 Proxy 并实现目标接口
 *   - 所有接口方法调用都转发到 InvocationHandler#invoke()
 *   - 因此 JDK 代理只能代理接口，无法代理没有接口的类
 *
 * 面试关键点：
 *   - 代理类与目标类无继承关系，通过接口解耦
 *   - invoke() 中 method.invoke(target, args) 调用的是真实对象，不是代理对象（避免死循环）
 */
public class LogInvocationHandler implements InvocationHandler {

    /** 真实目标对象 */
    private final Object target;

    public LogInvocationHandler(Object target) {
        this.target = target;
    }

    /**
     * @param proxy  生成的代理对象（$Proxy0 实例），通常不用
     * @param method 被调用的接口方法
     * @param args   方法参数
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.currentTimeMillis();
        System.out.println("[JDK Proxy] Before  -> " + method.getName());

        Object result;
        try {
            // 调用真实对象的方法
            result = method.invoke(target, args);
        } catch (Exception e) {
            System.out.println("[JDK Proxy] Exception -> " + e.getCause().getMessage());
            throw e.getCause();
        } finally {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[JDK Proxy] After   -> " + method.getName() + ", cost=" + cost + "ms");
        }

        return result;
    }
}
