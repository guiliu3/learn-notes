package com.learn.jvm.classloader;

import com.learn.jvm.classloader.spi.MessageSender;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;

/**
 * 示例：SPI 机制与线程上下文类加载器
 * 对应笔记：jvm/03-类加载机制.md —— 三、双亲委派模型 - 打破场景1
 *
 * 演示：
 *   1. SPI 标准用法（ServiceLoader 内部自动使用上下文类加载器）
 *   2. 模拟"Bootstrap 无法加载 classpath 实现类"的根本矛盾
 *   3. 线程上下文类加载器如何解决这个矛盾
 *   4. 手动还原 ServiceLoader 核心逻辑
 *
 * SPI 约定文件位置：
 *   src/main/resources/META-INF/services/com.learn.jvm.classloader.spi.MessageSender
 */
public class SpiDemo {

    public static void main(String[] args) throws Exception {

        // ===== 1. SPI 标准用法 =====
        System.out.println("===== 1. SPI 标准用法（ServiceLoader）=====");
        ServiceLoader<MessageSender> senders = ServiceLoader.load(MessageSender.class);
        for (MessageSender sender : senders) {
            System.out.println("发现实现类: " + sender.getClass().getName()
                    + "，加载器: " + sender.getClass().getClassLoader());
            sender.send("Hello SPI");
        }
        // ServiceLoader.load() 内部就是用线程上下文类加载器（AppClassLoader）加载实现类

        // ===== 2. 还原根本矛盾：上层加载器无法加载下层的类 =====
        System.out.println("\n===== 2. 模拟矛盾：Bootstrap 无法加载 classpath 里的实现类 =====");

        // 用 parent=null 的 URLClassLoader 来模拟"只认识 rt.jar 的 Bootstrap"
        // 它找不到 classpath 下的实现类
        URLClassLoader bootstrapLike = new URLClassLoader(new URL[0], null);
        try {
            bootstrapLike.loadClass("com.learn.jvm.classloader.spi.impl.EmailSender");
        } catch (ClassNotFoundException e) {
            System.out.println("Bootstrap-like 加载器找不到实现类: " + e.getMessage());
            System.out.println("这就是 DriverManager（Bootstrap加载）无法直接加载 MySQL Driver 的原因");
        }
        bootstrapLike.close();

        // ===== 3. 线程上下文类加载器解决矛盾 =====
        System.out.println("\n===== 3. 线程上下文类加载器解决矛盾 =====");

        // 查看默认的上下文类加载器
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        System.out.println("默认上下文类加载器: " + contextLoader);
        // 输出 AppClassLoader，能访问 classpath

        // 用上下文类加载器加载实现类（成功）
        Class<?> implClass = Class.forName(
                "com.learn.jvm.classloader.spi.impl.EmailSender",
                true,
                contextLoader  // 关键：用 AppClassLoader，不用 Bootstrap
        );
        System.out.println("上下文加载器成功加载: " + implClass.getName());

        // ===== 4. 手动还原 ServiceLoader 核心逻辑 =====
        System.out.println("\n===== 4. ServiceLoader 核心逻辑还原 =====");
        loadSpiManually();

        // ===== 5. 临时替换上下文类加载器（框架层常见做法）=====
        System.out.println("\n===== 5. 临时替换上下文类加载器 =====");
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            // 框架在执行某段逻辑前，切换成特定的 ClassLoader
            Thread.currentThread().setContextClassLoader(
                    new URLClassLoader(
                            new URL[]{SpiDemo.class.getResource("/")},
                            original  // 仍以 original 为父，保持双亲委派
                    )
            );
            System.out.println("切换后上下文加载器: "
                    + Thread.currentThread().getContextClassLoader());
        } finally {
            // 必须还原，否则影响后续逻辑（线程池场景下尤其重要）
            Thread.currentThread().setContextClassLoader(original);
            System.out.println("已还原上下文加载器: "
                    + Thread.currentThread().getContextClassLoader());
        }
    }

    /**
     * 手动模拟 ServiceLoader 的核心加载逻辑
     * 对应 JDK 源码：java.util.ServiceLoader#LazyIterator#nextService()
     */
    private static void loadSpiManually() throws Exception {
        String spiFileName = "META-INF/services/" + MessageSender.class.getName();

        // 第一步：拿线程上下文类加载器（AppClassLoader）
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        // 第二步：扫描所有 jar/classpath 下的 SPI 约定文件
        java.util.Enumeration<URL> resources = loader.getResources(spiFileName);
        System.out.println("找到 SPI 配置文件: " + spiFileName);

        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            System.out.println("  配置文件路径: " + url);

            // 第三步：读取文件中的实现类全限定名
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // 第四步：用 AppClassLoader 加载实现类（而非 Bootstrap）
                Class<?> implClass = Class.forName(line, true, loader);
                MessageSender instance = (MessageSender) implClass.newInstance();
                System.out.println("  手动加载实现: " + line
                        + "，加载器: " + implClass.getClassLoader());
                instance.send("手动 SPI 调用");
            }
            reader.close();
        }
    }
}
