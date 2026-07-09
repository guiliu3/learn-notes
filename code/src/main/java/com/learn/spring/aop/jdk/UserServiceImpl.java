package com.learn.spring.aop.jdk;

/**
 * 目标类（被代理对象）
 */
public class UserServiceImpl implements UserService {

    @Override
    public String findById(Long id) {
        System.out.println("[UserService] findById, id=" + id);
        return "User#" + id;
    }

    @Override
    public void save(String username) {
        System.out.println("[UserService] save, username=" + username);
    }
}
