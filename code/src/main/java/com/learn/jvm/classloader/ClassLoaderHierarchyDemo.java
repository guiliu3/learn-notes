package com.learn.jvm.classloader;

/**
 * 示例：类加载器层次结构
 * 对应笔记：jvm/03-类加载机制.md —— 二、类加载器分类
 *
 * 演示：
 *   1. 三层加载器的层次关系
 *   2. getClassLoader() 返回 null 代表 Bootstrap
 *   3. SystemClassLoader 就是 AppClassLoader
 */
public class ClassLoaderHierarchyDemo {

    public static void main(String[] args) {

        // ===== 1. 打印三层加载器 =====
        ClassLoader app = ClassLoaderHierarchyDemo.class.getClassLoader();
        ClassLoader ext = app.getParent();
        ClassLoader bootstrap = ext.getParent();

        System.out.println("===== 类加载器层次 =====");
        System.out.println("AppClassLoader    : " + app);
        System.out.println("ExtClassLoader    : " + ext);
        System.out.println("BootstrapLoader   : " + bootstrap);
        // Bootstrap 在 Java 层无对象，输出 null

        // ===== 2. JDK 核心类由 Bootstrap 加载 =====
        System.out.println("\n===== 核心类的加载器 =====");
        System.out.println("String  加载器: " + String.class.getClassLoader());
        // 输出 null → Bootstrap 加载
        System.out.println("HashMap 加载器: " + java.util.HashMap.class.getClassLoader());
        // 输出 null → Bootstrap 加载

        // ===== 3. 用户类由 AppClassLoader 加载 =====
        System.out.println("\n===== 用户类的加载器 =====");
        System.out.println("本类加载器: " + ClassLoaderHierarchyDemo.class.getClassLoader());

        // ===== 4. SystemClassLoader 就是 AppClassLoader =====
        System.out.println("\n===== SystemClassLoader =====");
        System.out.println("getSystemClassLoader(): " + ClassLoader.getSystemClassLoader());
        System.out.println("与 AppClassLoader 相同: "
                + (ClassLoader.getSystemClassLoader() == app));
        // 输出 true
    }
}
