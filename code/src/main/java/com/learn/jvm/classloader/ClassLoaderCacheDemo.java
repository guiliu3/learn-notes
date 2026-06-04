package com.learn.jvm.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 示例：类加载器缓存验证
 * 对应笔记：jvm/03-类加载机制.md —— 三、双亲委派模型
 *
 * 演示：
 *   1. 同一 ClassLoader 有独立缓存，同一个类只加载一次
 *   2. 不同 ClassLoader 各自缓存独立，互不影响
 *   3. 双亲委派让缓存能真正命中（避免重复加载的根本原因）
 */
public class ClassLoaderCacheDemo {

    public static void main(String[] args) throws Exception {

        // ===== 1. 缓存让大量 forName 几乎不耗时 =====
        System.out.println("===== 缓存性能验证 =====");
        long start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            Class.forName("com.learn.jvm.classloader.ClassLoaderCacheDemo");
        }
        long cost = (System.nanoTime() - start) / 1_000_000;
        System.out.println("10万次 forName 耗时: " + cost + "ms（缓存命中，几乎为0）");

        // ===== 2. 同一 ClassLoader 两次加载，返回同一个 Class 对象 =====
        System.out.println("\n===== 同一加载器，同一 Class 对象 =====");
        Class<?> c1 = Class.forName("com.learn.jvm.classloader.ClassLoaderCacheDemo");
        Class<?> c2 = Class.forName("com.learn.jvm.classloader.ClassLoaderCacheDemo");
        System.out.println("c1 == c2 : " + (c1 == c2));   // true

        // ===== 3. 不同 ClassLoader，各自独立缓存 =====
        System.out.println("\n===== 不同加载器，各自独立缓存 =====");
        URL url = ClassLoaderCacheDemo.class.getResource("/");

        URLClassLoader loaderA = new URLClassLoader(new URL[]{url}, null);
        URLClassLoader loaderB = new URLClassLoader(new URL[]{url}, null);

        Class<?> fromA = loaderA.loadClass("com.learn.jvm.classloader.ClassLoaderCacheDemo");
        Class<?> fromB = loaderB.loadClass("com.learn.jvm.classloader.ClassLoaderCacheDemo");

        System.out.println("loaderA 缓存的 Class hashCode: " + System.identityHashCode(fromA));
        System.out.println("loaderB 缓存的 Class hashCode: " + System.identityHashCode(fromB));
        System.out.println("fromA == fromB : " + (fromA == fromB));  // false，各自独立

        // loaderA 再次加载，命中自己的缓存
        Class<?> fromA2 = loaderA.loadClass("com.learn.jvm.classloader.ClassLoaderCacheDemo");
        System.out.println("\nloaderA 再次加载，命中缓存: " + (fromA == fromA2));  // true

        // ===== 4. 结论：双亲委派 + 缓存的协作关系 =====
        System.out.println("\n===== 结论 =====");
        System.out.println("缓存是避免重复加载的直接手段");
        System.out.println("双亲委派保证同一个类的加载请求汇聚到同一个加载器");
        System.out.println("两者协作 → 同一个类全局只有一个 Class 对象");

        loaderA.close();
        loaderB.close();
    }
}
