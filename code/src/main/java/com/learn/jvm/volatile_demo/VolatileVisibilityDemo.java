package com.learn.jvm.volatile_demo;

/**
 * 对比一：可见性
 * 对应笔记：jvm/04-JUC并发.md —— 二、volatile
 *
 * 结论：
 *   没有 volatile → 线程A 读的是工作内存缓存，看不到线程B的修改，死循环
 *   加了 volatile → 每次从主内存读，线程A 能立即感知到修改，正常退出
 *
 */
public class VolatileVisibilityDemo {

    // ===== 场景一：没有 volatile =====
    static boolean flagNoVolatile = false;

    // ===== 场景二：有 volatile =====
    static volatile boolean flagWithVolatile = false;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("======================================");
        System.out.println("  对比一：volatile 可见性");
        System.out.println("======================================");

        // ----- 先演示有 volatile 的情况 -----
        System.out.println("\n--- 场景一：有 volatile ---");
        flagWithVolatile = false;

        Thread readerWithVolatile = new Thread(() -> {
            System.out.println("[线程A] 开始循环读取 flagWithVolatile...");
            long count = 0;
            while (!flagWithVolatile) {
                count++;
            }
            System.out.println("[线程A] 感知到 flag=true，退出循环，循环了 " + count + " 次");
        }, "ReaderThread-Volatile");

        readerWithVolatile.start();
        Thread.sleep(500); // 让线程A先跑起来

        System.out.println("[线程B] 将 flagWithVolatile 设为 true");
        flagWithVolatile = true;
        readerWithVolatile.join(2000); // 最多等2秒

        if (readerWithVolatile.isAlive()) {
            System.out.println("[结果] 线程A 仍在运行（不应出现）");
            readerWithVolatile.interrupt();
        } else {
            System.out.println("[结果] 线程A 正常退出 ✓");
        }

        // ----- 再演示没有 volatile 的情况 -----
        System.out.println("\n--- 场景二：没有 volatile ---");
        flagNoVolatile = false;

        Thread readerNoVolatile = new Thread(() -> {
            System.out.println("[线程A] 开始循环读取 flagNoVolatile...");
            long count = 0;
            // JIT 编译后，flag 可能被缓存到寄存器，不再重新读主内存
            while (!flagNoVolatile) {
                count++;
                // 注意：如果在循环里加 System.out.println 会触发同步，反而让可见性生效
                // 所以这里故意不打印，让 JIT 优化发挥作用
            }
            System.out.println("[线程A] 感知到 flag=true，退出循环，循环了 " + count + " 次");
        }, "ReaderThread-NoVolatile");

        readerNoVolatile.setDaemon(true); // 设为守护线程，主线程结束它自动结束
        readerNoVolatile.start();
        Thread.sleep(500);

        System.out.println("[线程B] 将 flagNoVolatile 设为 true");
        flagNoVolatile = true;

        readerNoVolatile.join(3000); // 等3秒

        if (readerNoVolatile.isAlive()) {
            System.out.println("[结果] 线程A 仍在死循环！看不到线程B的修改");
            System.out.println("[原因] 没有 volatile，JIT 把 flag 缓存在寄存器中");
            System.out.println("[原因] 线程A 读的是工作内存副本，不是主内存的最新值");
        } else {
            System.out.println("[结果] 线程A 退出了（当前JVM/JIT优化程度下碰巧可见）");
        }

        System.out.println("\n======================================");
        System.out.println("  结论：volatile 强制每次从主内存读取");
        System.out.println("        不加 volatile，JIT 可能缓存变量");
        System.out.println("======================================");
    }
}
