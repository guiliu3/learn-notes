package com.learn.jvm.volatile_demo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对比二：原子性
 * 对应笔记：jvm/04-JUC并发.md —— 二、volatile
 *
 * 结论：
 *   volatile int i; i++    → 不保证原子性，结果小于预期（丢失更新）
 *   AtomicInteger           → 保证原子性，结果精确
 *   synchronized            → 保证原子性，结果精确
 *
 * 运行方式：直接右键 Run main()
 * 预期输出：
 *   volatile i++  结果 < 10000（每次不同，体现丢失更新）
 *   AtomicInteger 结果 = 10000（精确）
 *   synchronized  结果 = 10000（精确）
 */
public class VolatileAtomicDemo {

    static volatile int volatileCount = 0;         // volatile，但 ++ 不是原子的
    static AtomicInteger atomicCount = new AtomicInteger(0);  // 原子类
    static int syncCount = 0;                       // 普通变量 + synchronized

    private static final int THREAD_COUNT  = 10;
    private static final int INC_PER_THREAD = 1000;
    private static final int EXPECTED = THREAD_COUNT * INC_PER_THREAD; // 期望值 10000

    public static void main(String[] args) throws InterruptedException {

        System.out.println("======================================");
        System.out.println("  对比二：volatile 不保证原子性");
        System.out.println("  " + THREAD_COUNT + " 个线程，每个线程自增 " + INC_PER_THREAD + " 次");
        System.out.println("  期望结果 = " + EXPECTED);
        System.out.println("======================================");

        // ----- 场景一：volatile int ++ -----
        volatileCount = 0;
        CountDownLatch latch1 = new CountDownLatch(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < INC_PER_THREAD; j++) {
                    volatileCount++;   // 读-改-写，三步，非原子
                }
                latch1.countDown();
            }).start();
        }
        latch1.await();
        System.out.println("\n[volatile  i++]  实际结果 = " + volatileCount
                + "  期望 = " + EXPECTED
                + (volatileCount == EXPECTED ? "  ✓ 正确" : "  ✗ 丢失更新！"));

        // ----- 场景二：AtomicInteger -----
        atomicCount.set(0);
        CountDownLatch latch2 = new CountDownLatch(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < INC_PER_THREAD; j++) {
                    atomicCount.incrementAndGet();  // CAS 原子操作
                }
                latch2.countDown();
            }).start();
        }
        latch2.await();
        System.out.println("[AtomicInteger]  实际结果 = " + atomicCount.get()
                + "  期望 = " + EXPECTED
                + (atomicCount.get() == EXPECTED ? "  ✓ 正确" : "  ✗ 丢失更新！"));

        // ----- 场景三：synchronized -----
        syncCount = 0;
        Object lock = new Object();
        CountDownLatch latch3 = new CountDownLatch(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < INC_PER_THREAD; j++) {
                    synchronized (lock) {
                        syncCount++;   // 锁保护，原子
                    }
                }
                latch3.countDown();
            }).start();
        }
        latch3.await();
        System.out.println("[synchronized ]  实际结果 = " + syncCount
                + "  期望 = " + EXPECTED
                + (syncCount == EXPECTED ? "  ✓ 正确" : "  ✗ 丢失更新！"));

        System.out.println("\n======================================");
        System.out.println("  结论：");
        System.out.println("  volatile 只保证可见性，i++ 三步之间无互斥");
        System.out.println("  多线程同时读到相同值，各自+1写回，丢失更新");
        System.out.println("  原子性需要 AtomicInteger(CAS) 或 synchronized");
        System.out.println("======================================");
    }
}
