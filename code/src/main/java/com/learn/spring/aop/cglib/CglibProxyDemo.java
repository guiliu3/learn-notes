package com.learn.spring.aop.cglib;

import net.sf.cglib.proxy.Enhancer;

/**
 * CGLIB 动态代理 Demo
 *
 * 生成代理对象步骤（Enhancer）：
 *   1. setSuperclass()   —— 指定目标类（代理类继承它）
 *   2. setCallback()     —— 设置拦截器（MethodInterceptor）
 *   3. create()          —— 生成代理对象（目标类的子类实例）
 *
 * 运行输出示意：
 *   [CGLIB Proxy] Before  -> findOrderById
 *   [OrderService] findOrderById, orderId=100
 *   [CGLIB Proxy] After   -> findOrderById, cost=1ms
 *   result: Order#100
 *   代理类名称: com.learn.spring.aop.cglib.OrderService$$EnhancerByCGLIB$$xxxxxx
 *   [OrderService] finalMethod —— CGLIB 无法拦截 final 方法  （无 Before/After 日志）
 */
public class CglibProxyDemo {

    public static void main(String[] args) {
        // 1. 创建 Enhancer（代理类生成器）
        Enhancer enhancer = new Enhancer();

        // 2. 设置父类（目标类）
        enhancer.setSuperclass(OrderService.class);

        // 3. 设置拦截器
        enhancer.setCallback(new LogMethodInterceptor());

        // 4. 生成代理对象（调用目标类无参构造）
        OrderService proxy = (OrderService) enhancer.create();

        // 5. 调用方法
        String result = proxy.findOrderById(100L);
        System.out.println("result: " + result);

        System.out.println("---");
        proxy.createOrder("iPhone");

        System.out.println("---");
        // final 方法：CGLIB 子类无法覆盖，拦截器不会触发
        proxy.finalMethod();

        System.out.println("---");
        // 观察代理类名称：包含 $$EnhancerByCGLIB$$
        System.out.println("代理类名称: " + proxy.getClass().getName());
        System.out.println("父类是否为 OrderService: " + (proxy.getClass().getSuperclass() == OrderService.class));
    }
}
