package com.learn.thread;

/**
 * 演示 TIMED_WAITING 状态。
 *
 * TIMED_WAITING 的典型场景：
 * 1. Thread.sleep(timeout)
 * 2. Object.wait(timeout)
 * 3. Thread.join(timeout)
 * 4. LockSupport.parkNanos/parkUntil
 */
public class ThreadState04TimedWaitingDemo {

    private static final Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread sleepThread = new Thread(() -> {
            print("调用 Thread.sleep(3000)");
            sleep(3000);
            print("sleep 结束");
        }, "sleep-thread");

        Thread waitTimeoutThread = new Thread(() -> {
            synchronized (LOCK) {
                print("调用 LOCK.wait(3000)");
                try {
                    LOCK.wait(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                print("wait 超时结束");
            }
        }, "wait-timeout-thread");

        sleepThread.start();
        waitTimeoutThread.start();

        Thread.sleep(500);

        System.out.println("sleepThread state       = " + sleepThread.getState());
        System.out.println("waitTimeoutThread state = " + waitTimeoutThread.getState());

        sleepThread.join();
        waitTimeoutThread.join();
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
