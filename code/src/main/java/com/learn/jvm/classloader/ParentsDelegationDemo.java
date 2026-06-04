package com.learn.jvm.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 示例：双亲委派模型验证
 * 对应笔记：jvm/03-类加载机制.md —— 三、双亲委派模型
 *
 * 演示：
 *   1. 双亲委派安全性：核心类无法被覆盖
 *   2. 同一加载器中，同一个类只加载一次（Class 对象唯一）
 *   3. 不同加载器加载同名类 → 不同 Class 对象 → 强转 ClassCastException
 */
public class ParentsDelegationDemo {

    public static void main(String[] args) throws Exception {

        // ===== 1. 双亲委派安全性 =====
        // 即使 classpath 下有自定义的 java.lang.String，
        // 加载请求会一直委托到 Bootstrap，Bootstrap 从 rt.jar 加载真正的 String
        System.out.println("===== 双亲委派安全性 =====");
        Class<?> stringClass = Class.forName("java.lang.String");
        System.out.println("String 加载器: " + stringClass.getClassLoader());
        // 输出 null → Bootstrap，自定义 String 根本没有机会被加载

        // ===== 2. 同一加载器，同一个类只加载一次 =====
        System.out.println("\n===== 同一类只加载一次 =====");
        Class<?> c1 = Class.forName("com.learn.jvm.classloader.ParentsDelegationDemo");
        Class<?> c2 = Class.forName("com.learn.jvm.classloader.ParentsDelegationDemo");
        System.out.println("c1 == c2 : " + (c1 == c2));          // true，同一个 Class 对象
        System.out.println("c1 hashCode: " + System.identityHashCode(c1));
        System.out.println("c2 hashCode: " + System.identityHashCode(c2));

        // ===== 3. 不同加载器加载同名类 → 两个独立类型 =====
        System.out.println("\n===== 不同加载器加载同名类 =====");

        // parent=null：跳过 AppClassLoader，让 findClass() 自己从 classpath 加载
        // 这样两个 loader 各自独立加载，不会命中对方的缓存
        // ClassLoader parent = ClassLoader.getSystemClassLoader();

        URL classesUrl = ParentsDelegationDemo.class.getResource("/");
        URLClassLoader loader1 = new URLClassLoader(new URL[]{classesUrl}, null);
        URLClassLoader loader2 = new URLClassLoader(new URL[]{classesUrl}, null);

        Class<?> fromLoader1 = loader1.loadClass("com.learn.jvm.classloader.ParentsDelegationDemo");
        Class<?> fromLoader2 = loader2.loadClass("com.learn.jvm.classloader.ParentsDelegationDemo");

        System.out.println("loader1 加载的 Class hashCode: " + System.identityHashCode(fromLoader1));
        System.out.println("loader2 加载的 Class hashCode: " + System.identityHashCode(fromLoader2));
        System.out.println("fromLoader1 == fromLoader2 : " + (fromLoader1 == fromLoader2));
        // 输出 false，两个不同的 Class 对象

        // 强转验证：虽然类名相同，但类型不兼容
        Object obj = fromLoader1.newInstance();
        try {
            // obj 是 loader1 加载的 ParentsDelegationDemo 类型
            // 当前上下文用 AppClassLoader 加载的 ParentsDelegationDemo 类型
            // 两者不同 → ClassCastException
            @SuppressWarnings("unused")
            ParentsDelegationDemo cast = (ParentsDelegationDemo) obj;
        } catch (ClassCastException e) {
            System.out.println("\nClassCastException 触发！");
            System.out.println("结论：类的唯一性 = 全限定名 + ClassLoader，缺一不可");
        }

        loader1.close();
        loader2.close();
    }
}
