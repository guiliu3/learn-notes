package com.learn.jvm.classloader;

/**
 * 示例：类初始化顺序与 <clinit> 执行时机
 * 对应笔记：jvm/03-类加载机制.md —— 二、各阶段详解 - 准备/初始化
 *
 * 演示：
 *   1. 准备阶段赋零值 vs 初始化阶段执行赋值代码
 *   2. static 块和 static 变量按声明顺序执行
 *   3. 父类先于子类初始化
 *   4. 编译期常量（final static）不触发类初始化
 *   5. 静态内部类单例 —— <clinit> 线程安全的实际应用
 */
public class ClassInitOrderDemo {

    // ===== 演示1：static块 和 static变量的执行顺序 =====
    static class InitOrderCase {
        static int a = 1;

        static {
            // 此时 a 已赋值为 1（声明在 static 块之前）
            System.out.println("  static块执行，a = " + a);  // 1
            b = 20;  // 可以赋值（后向引用允许赋值，不允许读取）
            // System.out.println(b); // 编译报错：非法前向引用
        }

        // b 的声明在 static 块之后，赋值语句也在块之后执行
        // 执行顺序：static块赋 b=20 → 赋值语句赋 b=10 → 最终 b=10
        static int b = 10;

        static {
            System.out.println("  第二个static块，b = " + b);  // 10，被后面的赋值覆盖
        }
    }

    // ===== 演示2：父类先于子类初始化 =====
    static class Parent {
        static String name = "Parent";

        static {
            System.out.println("  Parent 静态块执行");
        }

        // 实例初始化块（每次 new 都执行）
        {
            System.out.println("  Parent 实例初始化块");
        }

        Parent() {
            System.out.println("  Parent 构造方法");
        }
    }

    static class Child extends Parent {
        static String name = "Child";

        static {
            System.out.println("  Child 静态块执行");
        }

        {
            System.out.println("  Child 实例初始化块");
        }

        Child() {
            // 隐式调用 super()，父类构造方法先执行
            System.out.println("  Child 构造方法");
        }
    }

    // ===== 演示3：编译期常量不触发类初始化 =====
    static class ConstantHolder {
        // 编译期常量：编译时直接内联到调用方字节码，访问时不触发类初始化
        public static final int COMPILE_TIME_CONST = 100;

        // 运行期常量：需要运行时计算，访问时触发类初始化
        public static final Integer RUNTIME_CONST = Integer.valueOf(200);

        static {
            System.out.println("  ConstantHolder 初始化！");
        }
    }

    // ===== 演示4：静态内部类单例（<clinit> 线程安全的实际应用）=====
    static class Singleton {
        private static int instanceCount = 0;

        private Singleton() {
            instanceCount++;
            System.out.println("  Singleton 实例创建，count=" + instanceCount);
        }

        /**
         * 静态内部类：
         *   - 只有调用 getInstance() 时才触发 Holder 的初始化（懒加载）
         *   - JVM 保证 <clinit>() 在多线程下只执行一次（线程安全）
         *   - 不需要 synchronized，性能比双重检查锁更好
         */
        private static class Holder {
            static final Singleton INSTANCE = new Singleton();
        }

        public static Singleton getInstance() {
            return Holder.INSTANCE;
        }
    }

    public static void main(String[] args) throws Exception {

        // ===== 1. static 块和变量执行顺序 =====
        System.out.println("===== 1. static 块和变量的执行顺序 =====");
        // 触发 InitOrderCase 初始化
        System.out.println("触发类初始化...");
        int ignored = InitOrderCase.a;
        System.out.println("最终结果：a=" + InitOrderCase.a + "，b=" + InitOrderCase.b);
        // a=1, b=10（不是20，static块里的赋值被后面的赋值语句覆盖）

        // ===== 2. 父类先于子类初始化 =====
        System.out.println("\n===== 2. 父类先于子类初始化 =====");
        System.out.println("new Child() 完整执行顺序：");
        new Child();
        // 执行顺序：
        // Parent static块 → Child static块（类初始化）
        // Parent 实例块 → Parent 构造 → Child 实例块 → Child 构造（对象初始化）

        // ===== 3. 编译期常量不触发类初始化 =====
        System.out.println("\n===== 3. 编译期常量 vs 运行期常量 =====");
        System.out.println("访问 COMPILE_TIME_CONST（不触发初始化）：");
        System.out.println("  值: " + ConstantHolder.COMPILE_TIME_CONST);
        // 不会看到 "ConstantHolder 初始化！"，因为常量已在编译时内联

        System.out.println("访问 RUNTIME_CONST（触发初始化）：");
        System.out.println("  值: " + ConstantHolder.RUNTIME_CONST);
        // 会看到 "ConstantHolder 初始化！"

        // ===== 4. 静态内部类单例 =====
        System.out.println("\n===== 4. 静态内部类单例 =====");
        System.out.println("调用 getInstance 前，Holder 类未初始化");
        Singleton s1 = Singleton.getInstance();
        Singleton s2 = Singleton.getInstance();
        System.out.println("s1 == s2 : " + (s1 == s2));  // true，全局唯一

        // ===== 5. 多线程验证 <clinit> 只执行一次 =====
        System.out.println("\n===== 5. 多线程验证 <clinit> 线程安全 =====");
        // 重置计数（仅演示，实际 <clinit> 只执行一次）
        System.out.println("10个线程同时调用 getInstance()，Singleton 只创建一次：");
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> Singleton.getInstance());
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        System.out.println("最终 instanceCount = " + Singleton.instanceCount);
        // 输出 1，JVM 保证 <clinit> 只执行一次
    }
}
