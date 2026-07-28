package com.learn.thread;

/**
 * 综合观察多个线程状态。
 *
 * 这个 demo 适合配合笔记理解：
 * NEW、RUNNABLE、BLOCKED、WAITING、TIMED_WAITING、TERMINATED。
 */
public class ThreadState05MonitorAllStatesDemo {

    private static final Object BLOCK_LOCK = new Object();
    private static final Object WAIT_LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread newThread = new Thread(() -> {
        }, "new-thread");

        Thread runnableThread = new Thread(() -> {
            long end = System.currentTimeMillis() + 4000;
            while (System.currentTimeMillis() < end) {
                // 空循环用于尽量保持 RUNNABLE，实际运行中也可能被操作系统调度。
            }
        }, "runnable-thread");

        Thread lockHolder = new Thread(() -> {
            synchronized (BLOCK_LOCK) {
                sleep(4000);
            }
        }, "lock-holder");

        Thread blockedThread = new Thread(() -> {
            synchronized (BLOCK_LOCK) {
                print("拿到锁后结束");
            }
        }, "blocked-thread");

        Thread waitingThread = new Thread(() -> {
            synchronized (WAIT_LOCK) {
                try {
                    WAIT_LOCK.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "waiting-thread");

        Thread timedWaitingThread = new Thread(() -> sleep(4000), "timed-waiting-thread");

        System.out.println("newThread initial state = " + newThread.getState());

        runnableThread.start();
        lockHolder.start();
        Thread.sleep(200);
        blockedThread.start();
        waitingThread.start();
        timedWaitingThread.start();

        Thread.sleep(500);

        printState(runnableThread);
        printState(lockHolder);
        printState(blockedThread);
        printState(waitingThread);
        printState(timedWaitingThread);

        synchronized (WAIT_LOCK) {
            WAIT_LOCK.notify();
        }

        runnableThread.join();
        lockHolder.join();
        blockedThread.join();
        waitingThread.join();
        timedWaitingThread.join();

        printState(runnableThread);
        printState(blockedThread);
        printState(waitingThread);
        printState(timedWaitingThread);
    }

    private static void printState(Thread thread) {
        System.out.printf("%s state = %s%n", thread.getName(), thread.getState());
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
