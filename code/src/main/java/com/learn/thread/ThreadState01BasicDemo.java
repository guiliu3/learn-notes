package com.learn.thread;

/**
 * 演示 NEW、RUNNABLE、TIMED_WAITING、TERMINATED 几种基础状态。
 *
 * 使用方式：
 * 1. 将本文件后缀从 .txt 改为 .java
 * 2. 文件名保持 ThreadState01BasicDemo.java
 * 3. 运行 main 方法
 */
public class ThreadState01BasicDemo {

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(() -> {
            print("子线程开始执行");

            try {
                print("子线程准备 sleep，此时会进入 TIMED_WAITING");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                print("子线程被中断");
            }

            print("子线程执行结束");
        }, "basic-state-thread");

        // 线程对象创建完成，但还没有 start，此时是 NEW。
        printState("start 前", thread);

        thread.start();

        // start 后，线程进入 RUNNABLE。注意 Java 的 RUNNABLE 包含 ready/running 两种操作系统层面的状态。
        printState("start 后", thread);

        Thread.sleep(500);
        printState("子线程 sleep 中", thread);

        thread.join();
        printState("join 等待结束后", thread);
    }

    private static void printState(String scene, Thread thread) {
        System.out.printf("[%s] %s state = %s%n", scene, thread.getName(), thread.getState());
    }

    private static void print(String message) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), message);
    }
}
