package com.learn.spring.aop.jdk;

import java.lang.reflect.Proxy;

/**
 * JDK 动态代理 Demo
 *
 * 生成代理对象三要素（Proxy.newProxyInstance）：
 *   1. ClassLoader       —— 加载代理类字节码
 *   2. Class<?>[] interfaces —— 代理类需要实现哪些接口
 *   3. InvocationHandler —— 方法拦截逻辑
 *
 * 运行输出示意：
 *   [JDK Proxy] Before  -> findById
 *   [UserService] findById, id=1
 *   [JDK Proxy] After   -> findById, cost=0ms
 *   result: User#1
 *   代理类名称: com.sun.proxy.$Proxy0
 */
public class JdkProxyDemo {

    public static void main(String[] args) {
        // 1. 真实对象
        UserService target = new UserServiceImpl();

        // 2. 创建 InvocationHandler，持有真实对象
        LogInvocationHandler handler = new LogInvocationHandler(target);

        // 3. 生成代理对象
        UserService proxy = (UserService) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),  // ClassLoader
                target.getClass().getInterfaces(),   // 目标类实现的接口
                handler                              // 增强逻辑
        );

        // 4. 通过代理对象调用方法
        String result = proxy.findById(1L);
        System.out.println("result: " + result);

        System.out.println("---");
        proxy.save("张三");

        System.out.println("---");
        // 观察代理类名称：$Proxy0，说明是运行期生成的字节码
        System.out.println("代理类名称: " + proxy.getClass().getName());
        System.out.println("是否实现了 UserService: " + (proxy instanceof UserService));
    }
}
