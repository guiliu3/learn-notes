package com.learn.jvm.atomic;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.*;

/**
 * 原子类使用示例
 * 对应笔记：jvm/04-JUC并发.md —— 四、CAS 与原子类
 *
 * 覆盖场景：
 *   1. AtomicInteger    —— 线程安全计数器
 *   2. AtomicReference  —— 线程安全对象引用更新
 *   3. AtomicStampedReference —— 解决 ABA 问题
 *   4. LongAdder        —— 高并发计数，对比 AtomicLong 性能
 *   5. AtomicIntegerFieldUpdater —— 不改字段类型，直接原子更新
 */
public class AtomicDemo {

    public static void main(String[] args) throws InterruptedException {
//        demo1_AtomicInteger();
//        demo2_AtomicReference();
        demo3_ABA_Problem();
//        demo4_LongAdder();
//        demo5_FieldUpdater();

    }

    // ===================================================================
    // 示例1：AtomicInteger —— 最常用，线程安全的整数操作
    // ===================================================================
    static void demo1_AtomicInteger() throws InterruptedException {
        System.out.println("========== 1. AtomicInteger ==========");

        AtomicInteger counter = new AtomicInteger(0);

        // 常用 API 演示
        System.out.println("初始值               : " + counter.get());
        System.out.println("getAndIncrement (i++): " + counter.getAndIncrement()); // 返回旧值0，然后变1
        System.out.println("incrementAndGet (++i): " + counter.incrementAndGet()); // 先变2，返回新值2
        System.out.println("getAndAdd(5)         : " + counter.getAndAdd(5));      // 返回旧值2，然后变7
        System.out.println("addAndGet(3)         : " + counter.addAndGet(3));      // 先变10，返回新值10
        System.out.println("getAndSet(100)       : " + counter.getAndSet(100));    // 返回旧值10，然后变100
        System.out.println("当前值               : " + counter.get());             // 100

        // compareAndSet：CAS 核心方法
        System.out.println("\n-- compareAndSet --");
        boolean success1 = counter.compareAndSet(100, 200); // 期望100，当前是100，成功
        System.out.println("CAS(100→200) 成功: " + success1 + "，当前值: " + counter.get());

        boolean success2 = counter.compareAndSet(100, 300); // 期望100，当前是200，失败
        System.out.println("CAS(100→300) 成功: " + success2 + "，当前值: " + counter.get());

        // 多线程场景：10个线程各自增1000次
        System.out.println("\n-- 多线程计数 --");
        AtomicInteger total = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) total.incrementAndGet();
                latch.countDown();
            }).start();
        }
        latch.await();
        System.out.println("10线程各增1000次，期望10000，实际: " + total.get());
    }

    // ===================================================================
    // 示例2：AtomicReference —— 原子更新对象引用
    // 场景：多线程下安全地替换对象，不用 synchronized
    // ===================================================================
    static void demo2_AtomicReference() {
        System.out.println("\n========== 2. AtomicReference ==========");

        // 用户对象，模拟配置热更新场景
        AtomicReference<String> configRef = new AtomicReference<>("config-v1");

        System.out.println("当前配置: " + configRef.get());

        // 线程安全地更新配置
        boolean updated = configRef.compareAndSet("config-v1", "config-v2");
        System.out.println("更新 v1→v2 成功: " + updated + "，当前: " + configRef.get());

        // 已经是 v2 了，用 v1 去 CAS 会失败
        boolean failed = configRef.compareAndSet("config-v1", "config-v3");
        System.out.println("更新 v1→v3 成功: " + failed  + "，当前: " + configRef.get());

        // 实际场景：多线程竞争更新，只有一个能成功
        AtomicReference<String> winner = new AtomicReference<>(null);
        for (int i = 0; i < 5; i++) {
            final String threadName = "线程" + i;
            new Thread(() -> {
                boolean win = winner.compareAndSet(null, threadName);
                if (win) System.out.println(threadName + " 抢到了资源！");
                else     System.out.println(threadName + " 抢失败，当前持有者: " + winner.get());
            }).start();
        }
    }

    // ===================================================================
    // 示例3：AtomicStampedReference —— 带版本号，解决 ABA 问题
    // ===================================================================
    static void demo3_ABA_Problem() throws InterruptedException {
        System.out.println("\n========== 3. ABA 问题与解决 ==========");

        // 注意：AtomicReference 用的是引用比较（==），不是值比较（equals）
        // Integer 缓存范围是 -128~127，超出范围每次装箱是新对象，== 会失败
        // 所以这里用 String，或者用缓存范围内的小整数
        // --- 先演示 ABA 问题 ---
        System.out.println("-- AtomicReference ABA 问题演示 --");
        AtomicReference<String> ref = new AtomicReference<>("A");

        // 线程1：读到 A，准备改成 C
        // 模拟线程1被暂停，线程2趁机 A→B→A
        String oldVal = ref.get();
        System.out.println("线程1 读到值: " + oldVal);

        // 线程2：A→B→A（改了两次，但最终值还是 A）
        ref.compareAndSet("A", "B");
        System.out.println("线程2 改为B: " + ref.get());
        ref.compareAndSet("B", "A");
        System.out.println("线程2 改回A: " + ref.get());

        // 线程1：CAS 成功！但它不知道中间发生过 A→B→A 的变化
        boolean success = ref.compareAndSet(oldVal, "C");
        System.out.println("线程1 CAS(A→C) 成功: " + success + "（不知道中间经历了A→B→A）");

        // --- 用 AtomicStampedReference 解决 ---
        System.out.println("\n-- AtomicStampedReference 解决 ABA --");
        // 初始值 A，初始版本号 0
        AtomicStampedReference<String> stampedRef =
                new AtomicStampedReference<>("A", 0);

        // 线程1：读到值 A，版本号 0
        int[] stampHolder = new int[1];
        String val = stampedRef.get(stampHolder);
        int stamp = stampHolder[0];
        System.out.println("线程1 读到: 值=" + val + "，版本号=" + stamp);

        // 线程2：A→B，版本号 0→1
        stampedRef.compareAndSet("A", "B", 0, 1);
        System.out.println("线程2 改为B: 值=" + stampedRef.getReference()
                + "，版本号=" + stampedRef.getStamp());
        // 线程2：B→A，版本号 1→2
        stampedRef.compareAndSet("B", "A", 1, 2);
        System.out.println("线程2 改回A: 值=" + stampedRef.getReference()
                + "，版本号=" + stampedRef.getStamp());

        // 线程1：拿着旧版本号 0 去 CAS，即使值还是 A，也失败！
        boolean stampedSuccess = stampedRef.compareAndSet(val, "C", stamp, stamp + 1);
        System.out.println("线程1 CAS(版本号0, A→C) 成功: " + stampedSuccess);
        System.out.println("当前: 值=" + stampedRef.getReference()
                + "，版本号=" + stampedRef.getStamp());
        System.out.println("结论：版本号不匹配（期望0，实际2），ABA 问题被识别！");
    }

    // ===================================================================
    // 示例4：LongAdder vs AtomicLong —— 高并发计数性能对比
    // ===================================================================
    static void demo4_LongAdder() throws InterruptedException {
        System.out.println("\n========== 4. LongAdder vs AtomicLong 性能 ==========");

        int threadCount = 50;
        int loopCount  = 100_000;

        // --- AtomicLong ---
        AtomicLong atomicLong = new AtomicLong(0);
        CountDownLatch latch1  = new CountDownLatch(threadCount);
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < loopCount; j++) atomicLong.incrementAndGet();
                latch1.countDown();
            }).start();
        }
        latch1.await();
        long cost1 = System.currentTimeMillis() - start1;
        System.out.println("AtomicLong  结果=" + atomicLong.get()
                + "，耗时=" + cost1 + "ms");

        // --- LongAdder ---
        LongAdder longAdder = new LongAdder();
        CountDownLatch latch2 = new CountDownLatch(threadCount);
        long start2 = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < loopCount; j++) longAdder.increment();
                latch2.countDown();
            }).start();
        }
        latch2.await();
        long cost2 = System.currentTimeMillis() - start2;
        System.out.println("LongAdder   结果=" + longAdder.sum()
                + "，耗时=" + cost2 + "ms");

        System.out.println("LongAdder 比 AtomicLong 快约: "
                + String.format("%.1f", (double) cost1 / cost2) + " 倍");
        System.out.println("原因：LongAdder 把竞争分散到 Cell[]，减少 CAS 失败自旋");
    }

    // ===================================================================
    // 示例5：AtomicIntegerFieldUpdater
    // 场景：已有对象的 volatile int 字段，不想改类型，直接原子更新
    // ===================================================================
    static void demo5_FieldUpdater() throws InterruptedException {
        System.out.println("\n========== 5. AtomicIntegerFieldUpdater ==========");

        // 创建更新器，指定类和字段名
        AtomicIntegerFieldUpdater<Order> updater =
                AtomicIntegerFieldUpdater.newUpdater(Order.class, "status");

        Order order = new Order(1, 0); // status=0 表示待支付
        System.out.println("初始状态: " + order);

        // 模拟支付：CAS 0（待支付）→ 1（已支付）
        boolean paid = updater.compareAndSet(order, 0, 1);
        System.out.println("支付成功: " + paid + "，" + order);

        // 重复支付：CAS 0→1 失败（已经是1了）
        boolean paidAgain = updater.compareAndSet(order, 0, 1);
        System.out.println("重复支付: " + paidAgain + "，" + order);

        // 多线程竞争支付，只有一个能成功
        Order order2 = new Order(2, 0);
        CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            final int tid = i;
            new Thread(() -> {
                boolean success = updater.compareAndSet(order2, 0, 1);
                if (success) System.out.println("线程" + tid + " 支付成功！");
                else         System.out.println("线程" + tid + " 支付失败（已被支付）");
                latch.countDown();
            }).start();
        }
        latch.await();
        System.out.println("最终状态: " + order2);
    }

    // 订单对象
    static class Order {
        int id;
        // 必须是 volatile，AtomicIntegerFieldUpdater 才能工作
        volatile int status;

        Order(int id, int status) {
            this.id = id;
            this.status = status;
        }

        @Override
        public String toString() {
            return "Order{id=" + id + ", status=" + status
                    + "(" + (status == 0 ? "待支付" : "已支付") + ")}";
        }
    }
}
