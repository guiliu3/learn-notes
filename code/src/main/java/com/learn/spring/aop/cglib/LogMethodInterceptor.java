package com.learn.spring.aop.cglib;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * CGLIB 动态代理核心：MethodInterceptor
 *
 * 原理：
 *   - CGLIB 在运行时生成目标类的子类字节码（ASM框架直接操作字节码）
 *   - 子类覆盖父类的非 final 方法，在覆盖方法中调用 MethodInterceptor#intercept()
 *   - 因此 CGLIB 无需目标类实现接口，但 final 类/方法无法被代理
 *
 * 面试关键点：
 *   - invokeSuper(obj, args) 调用父类（真实对象）的方法，避免无限递归
 *   - 不要用 method.invoke(obj, args)，那样会再次触发代理导致死循环
 *   - Spring 默认对有接口的 Bean 用 JDK 代理，无接口用 CGLIB；
 *     Spring Boot 2.x 起默认全部使用 CGLIB（proxyTargetClass=true）
 */
public class LogMethodInterceptor implements MethodInterceptor {

    /**
     * @param obj    CGLIB 生成的代理对象（目标类的子类实例）
     * @param method 被拦截的方法（反射 Method）
     * @param args   方法参数
     * @param proxy  MethodProxy，比反射调用性能更高
     */
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        long start = System.currentTimeMillis();
        System.out.println("[CGLIB Proxy] Before  -> " + method.getName());

        Object result;
        try {
            // 关键：invokeSuper 调用父类真实方法，不会触发二次代理
            result = proxy.invokeSuper(obj, args);
        } catch (Exception e) {
            System.out.println("[CGLIB Proxy] Exception -> " + e.getMessage());
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[CGLIB Proxy] After   -> " + method.getName() + ", cost=" + cost + "ms");
        }

        return result;
    }
}
