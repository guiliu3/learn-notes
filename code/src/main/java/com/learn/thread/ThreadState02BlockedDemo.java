package com.learn.thread;

/**
 * 演示 BLOCKED 状态。
 *
 * BLOCKED 的典型场景：
 * 一个线程已经进入 synchronized 临界区，另一个线程也想进入同一把锁保护的代码块，
 * 但锁被占用，所以第二个线程进入 BLOCKED。
 */
public class ThreadState02BlockedDemo {

    private static final Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread holder = new Thread(() -> {
            synchronized (LOCK) {
                print("拿到锁，开始长时间占用锁");
                sleep(5000);
                print("释放锁");
            }
        }, "lock-holder");

        Thread blocked = new Thread(() -> {
            print("准备竞争锁");
            synchronized (LOCK) {
                print("终于拿到锁");
            }
        }, "blocked-thread");

        holder.start();
        Thread.sleep(300);

        blocked.start();
        Thread.sleep(300);

        System.out.println("holder state  = " + holder.getState());
        System.out.println("blocked state = " + blocked.getState());

        holder.join();
        blocked.join();

        System.out.println("blocked final state = " + blocked.getState());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void print(String message) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), message);
    }
}
