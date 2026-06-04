package com.learn.jvm.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 示例：自定义类加载器
 * 对应笔记：jvm/03-类加载机制.md —— 五、自定义类加载器
 *
 * 演示：
 *   1. 重写 findClass() —— 不破坏双亲委派（推荐方式）
 *   2. 重写 loadClass() —— 破坏双亲委派（Tomcat 的做法）
 *   3. 模拟加密 Class 文件的加载（XOR 解密）
 *
 * 结论：
 *   - 只重写 findClass：仅扩展"自己找"的逻辑，委派流程不变
 *   - 重写 loadClass ：才能真正改变委派方向
 */
public class CustomClassLoaderDemo {

    // ===== 实现一：标准自定义加载器（不破坏双亲委派）=====
    static class PathClassLoader extends ClassLoader {

        private final String classPath;

        PathClassLoader(String classPath) {
            // 显式指定 parent = AppClassLoader，保持双亲委派
            super(ClassLoader.getSystemClassLoader());
            this.classPath = classPath;
        }

        /**
         * 只重写 findClass：
         * loadClass() 先委托父加载器，父找不到时才调用此方法
         */
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = readClassBytes(name);
            if (bytes == null) {
                throw new ClassNotFoundException("找不到类文件: " + name);
            }
            // 将字节数组注册为 JVM 中的 Class 对象
            return defineClass(name, bytes, 0, bytes.length);
        }

        private byte[] readClassBytes(String name) {
            String path = classPath + "/" + name.replace('.', '/') + ".class";
            try {
                return Files.readAllBytes(Paths.get(path));
            } catch (IOException e) {
                return null;
            }
        }
    }

    // ===== 实现二：模拟 Tomcat，重写 loadClass 破坏双亲委派 =====
    static class TomcatLikeClassLoader extends ClassLoader {

        private final String classPath;

        TomcatLikeClassLoader(String classPath) {
            super(ClassLoader.getSystemClassLoader());
            this.classPath = classPath;
        }

        /**
         * 重写 loadClass：自己先尝试，找不到才委托父加载器
         * 这与标准双亲委派方向相反
         */
        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {

            synchronized (getClassLoadingLock(name)) {
                // 先查自己的缓存
                Class<?> c = findLoadedClass(name);
                if (c != null) return c;

                // 自己先尝试加载（与标准委派相反）
                try {
                    c = findClass(name);
                    if (resolve) resolveClass(c);
                    System.out.println("[TomcatLike] 自己加载成功: " + name);
                    return c;
                } catch (ClassNotFoundException ignored) {
                    // 自己找不到，再委托父加载器
                }

                // 委托父加载器（兜底，比如 java.lang.* 还是要走 Bootstrap）
                return super.loadClass(name, resolve);
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String path = classPath + "/" + name.replace('.', '/') + ".class";
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(path));
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name);
            }
        }
    }

    // ===== 实现三：加密 Class 文件加载器（XOR 解密）=====
    static class DecryptClassLoader extends ClassLoader {

        private final String classPath;
        private final byte xorKey;

        DecryptClassLoader(String classPath, byte xorKey) {
            super(ClassLoader.getSystemClassLoader());
            this.classPath = classPath;
            this.xorKey = xorKey;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] encrypted = readEncryptedBytes(name);
            if (encrypted == null) throw new ClassNotFoundException(name);
            byte[] decrypted = xorDecrypt(encrypted);
            return defineClass(name, decrypted, 0, decrypted.length);
        }

        private byte[] xorDecrypt(byte[] data) {
            byte[] result = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                result[i] = (byte) (data[i] ^ xorKey);
            }
            return result;
        }

        private byte[] readEncryptedBytes(String name) {
            // 约定加密文件后缀为 .enc
            String path = classPath + "/" + name.replace('.', '/') + ".class.enc";
            try {
                return Files.readAllBytes(Paths.get(path));
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static void main(String[] args) throws Exception {

        String targetPath = System.getProperty("user.dir") + "/target/classes";
        System.out.println("classes 路径: " + targetPath);

        // ===== 演示一：PathClassLoader（不破坏双亲委派）=====
        System.out.println("\n===== 1. PathClassLoader（不破坏双亲委派）=====");
        PathClassLoader pathLoader = new PathClassLoader(targetPath);
        System.out.println("父加载器: " + pathLoader.getParent());

        // 因为 parent=AppClassLoader，AppClassLoader 先找到了 ClassLoaderHierarchyDemo
        // findClass() 根本不会被调用
        Class<?> clazz = pathLoader.loadClass(
                "com.learn.jvm.classloader.ClassLoaderHierarchyDemo");
        System.out.println("实际加载器: " + clazz.getClassLoader());
        // 输出 AppClassLoader，不是 PathClassLoader
        System.out.println("↑ 父加载器（AppClassLoader）先找到，findClass 未被调用");

        // 设 parent=null，绕开 AppClassLoader，让 findClass 被调用
        System.out.println("\n--- parent=null 时 findClass 才会被调用 ---");
        PathClassLoader loaderNoParent = new PathClassLoader(targetPath) {
            { /* 通过匿名子类在构造后用反射修改 parent 字段 */
                try {
                    java.lang.reflect.Field f = ClassLoader.class.getDeclaredField("parent");
                    f.setAccessible(true);
                    f.set(this, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Class<?> clazz2 = loaderNoParent.loadClass(
                "com.learn.jvm.classloader.ClassLoaderHierarchyDemo");
        System.out.println("实际加载器: " + clazz2.getClassLoader());
        // 输出 PathClassLoader，findClass 被调用了

        // ===== 演示二：TomcatLikeClassLoader（破坏双亲委派）=====
        System.out.println("\n===== 2. TomcatLikeClassLoader（破坏双亲委派）=====");
        TomcatLikeClassLoader tomcatLoader = new TomcatLikeClassLoader(targetPath);
        Class<?> tomcatLoaded = tomcatLoader.loadClass(
                "com.learn.jvm.classloader.ClassLoaderHierarchyDemo");
        System.out.println("实际加载器: " + tomcatLoaded.getClassLoader());
        // 输出 TomcatLikeClassLoader，自己先加载成功

        // JDK 核心类仍然走父加载器（Bootstrap）
        Class<?> stringClass = tomcatLoader.loadClass("java.lang.String");
        System.out.println("String 加载器: " + stringClass.getClassLoader());
        // 输出 null（Bootstrap），核心类不受影响

        // ===== 演示三：URLClassLoader 加载远程/外部 jar =====
        System.out.println("\n===== 3. URLClassLoader 加载外部路径 =====");
        URL[] urls = { new URL("file:" + targetPath + "/") };
        URLClassLoader urlLoader = new URLClassLoader(urls, null);
        System.out.println("URLClassLoader: " + urlLoader);
        System.out.println("可以加载来自 HTTP URL、JAR URL、FILE URL 的类");
        urlLoader.close();
    }
}
