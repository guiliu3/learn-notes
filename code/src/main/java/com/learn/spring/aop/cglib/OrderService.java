package com.learn.spring.aop.cglib;

/**
 * 目标类（无接口）—— CGLIB 可代理没有实现任何接口的普通类
 * 对应笔记：spring/02-Spring AOP与事务管理.md
 */
public class OrderService {

    public String findOrderById(Long orderId) {
        System.out.println("[OrderService] findOrderById, orderId=" + orderId);
        return "Order#" + orderId;
    }

    public void createOrder(String product) {
        System.out.println("[OrderService] createOrder, product=" + product);
    }

    /**
     * final 方法无法被 CGLIB 代理（子类无法覆盖），仍会调用原始方法
     */
    public final void finalMethod() {
        System.out.println("[OrderService] finalMethod —— CGLIB 无法拦截 final 方法");
    }
}
