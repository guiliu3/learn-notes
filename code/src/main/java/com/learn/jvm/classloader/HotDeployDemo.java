package com.learn.jvm.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;

/**
 * 示例：热部署原理
 * 对应笔记：jvm/03-类加载机制.md —— 三、双亲委派模型 - 打破场景4
 *
 * 演示：
 *   1. 同一个 ClassLoader 无法重复加载同一个类（缓存命中）
 *   2. 创建新 ClassLoader 实现"重新加载"（热部署核心原理）
 *   3. 旧 ClassLoader 被 GC 回收 → 类卸载 → Metaspace 释放
 *   4. 旧 ClassLoader 有残留引用 → Metaspace 泄漏（热部署常见问题）
 */
public class HotDeployDemo {

    public static void main(String[] args) throws Exception {
        URL classesUrl = HotDeployDemo.class.getResource("/");

        // ===== 1. 同一 ClassLoader 无法重复加载同一个类 =====
        System.out.println("===== 1. 同一 ClassLoader 无法重新加载类 =====");
        URLClassLoader loader1 = new URLClassLoader(new URL[]{classesUrl}, null);

        Class<?> v1 = loader1.loadClass("com.learn.jvm.classloader.HotDeployDemo");
        Class<?> v1Again = loader1.loadClass("com.learn.jvm.classloader.HotDeployDemo");

        System.out.println("第一次加载 hashCode : " + System.identityHashCode(v1));
        System.out.println("再次加载  hashCode  : " + System.identityHashCode(v1Again));
        System.out.println("是否同一个 Class    : " + (v1 == v1Again));
        // true，命中缓存，类没有重新加载

        // ===== 2. 创建新 ClassLoader 实现热部署 =====
        System.out.println("\n===== 2. 新 ClassLoader → 实现热部署 =====");

        // 模拟热部署：关闭旧加载器，创建新加载器
        loader1.close();
        // 断开对旧 ClassLoader 的强引用（实际 Tomcat 会重新初始化 WebAppClassLoader）
        WeakRef weakRef = new WeakRef(loader1);
        loader1 = null;

        // 创建新的 ClassLoader（模拟 Tomcat 部署新版本）
        URLClassLoader loader2 = new URLClassLoader(new URL[]{classesUrl}, null);
        Class<?> v2 = loader2.loadClass("com.learn.jvm.classloader.HotDeployDemo");

        System.out.println("loader2 加载的 Class hashCode: " + System.identityHashCode(v2));
        System.out.println("与 v1 不同（新 ClassLoader 独立缓存）: " + (v1 != v2));

        // ===== 3. GC 触发类卸载 =====
        System.out.println("\n===== 3. GC 与类卸载 =====");
        System.gc();
        Thread.sleep(100); // 给 GC 时间执行
        System.out.println("触发 GC 后，旧 ClassLoader（loader1）满足回收条件:");
        System.out.println("  - loader1 引用已置 null ✓");
        System.out.println("  - 旧 Class 对象无引用 ✓");
        System.out.println("  - 旧 Class 加载的实例对象无引用 ✓");
        System.out.println("→ 旧 ClassLoader 被 GC 回收 → 类卸载 → Metaspace 空间释放");

        // ===== 4. 模拟 Metaspace 泄漏场景 =====
        System.out.println("\n===== 4. Metaspace 泄漏根因（热部署常见问题）=====");
        simulateLeak(classesUrl);

        loader2.close();
    }

    /**
     * 模拟热部署内存泄漏场景
     * 根因：静态集合持有旧 ClassLoader 加载的对象引用，导致旧 ClassLoader 无法被 GC
     */
    private static void simulateLeak(URL classesUrl) throws Exception {
        // 模拟一个全局静态缓存（常见于各种框架，如 Spring、线程池等）
        java.util.List<Object> globalCache = new java.util.ArrayList<>();

        URLClassLoader leakyLoader = new URLClassLoader(new URL[]{classesUrl}, null);
        Class<?> leakyClass = leakyLoader.loadClass("com.learn.jvm.classloader.HotDeployDemo");
        Object instance = leakyClass.newInstance();

        // 问题：把旧加载器加载的对象放入全局静态缓存
        globalCache.add(instance);

        // 热部署：尝试关闭旧加载器
        leakyLoader.close();
        // leakyLoader 置 null，但 globalCache 仍然持有 instance 的引用
        // instance → leakyClass → leakyLoader，整条引用链不会断
        // → leakyLoader 无法被 GC → 类无法卸载 → Metaspace 持续增长
        leakyLoader = null;

        System.gc();
        Thread.sleep(100);

        System.out.println("泄漏场景：globalCache 持有旧 ClassLoader 加载的对象");
        System.out.println("引用链：globalCache → instance → leakyClass → leakyLoader");
        System.out.println("结果：leakyLoader 无法 GC → 类无法卸载 → Metaspace OOM");
        System.out.println("解决：热部署前清空缓存，或使用 WeakReference 持有对象");

        // 清空缓存才能让 GC 回收
        globalCache.clear();
        System.gc();
        System.out.println("清空缓存后，GC 可以回收旧 ClassLoader，Metaspace 正常释放");
    }

    /** 用弱引用验证 GC 是否回收了对象（仅演示用） */
    static class WeakRef {
        private final java.lang.ref.WeakReference<Object> ref;
        WeakRef(Object obj) {
            this.ref = new java.lang.ref.WeakReference<>(obj);
        }
        boolean isGCed() {
            return ref.get() == null;
        }
    }
}
