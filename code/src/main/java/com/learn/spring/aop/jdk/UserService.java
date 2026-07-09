package com.learn.spring.aop.jdk;

/**
 * 目标接口 —— JDK动态代理要求目标类必须实现接口
 * 对应笔记：spring/02-Spring AOP与事务管理.md
 */
public interface UserService {

    String findById(Long id);

    void save(String username);
}
