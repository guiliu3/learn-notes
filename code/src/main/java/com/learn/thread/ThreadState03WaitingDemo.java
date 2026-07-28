package com.learn.thread;

/**
 * 演示 WAITING 状态。
 *
 * WAITING 的典型场景：
 * 1. Object.wait()
 * 2. Thread.join() 无超时时间
 * 3. LockSupport.park()
 *
 * 这个 demo 使用 Object.wait()，需要另一个线程 notify 才能继续执行。
 */
public class ThreadState03WaitingDemo {

    private static final Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread waitingThread = new Thread(() -> {
            synchronized (LOCK) {
                print("准备调用 wait，释放锁并进入 WAITING");
                try {
                    LOCK.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    print("等待时被中断");
                }
                print("被 notify 唤醒后继续执行");
            }
        }, "waiting-thread");

        waitingThread.start();
        Thread.sleep(500);

        System.out.println("waitingThread state = " + waitingThread.getState());

        Thread notifier = new Thread(() -> {
            synchronized (LOCK) {
                print("准备 notify");
                LOCK.notify();
            }
        }, "notifier-thread");

        notifier.start();

        waitingThread.join();
        notifier.join();

        System.out.println("waitingThread final state = " + waitingThread.getState());
    }

    private static void print(String message) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), message);
    }
}
