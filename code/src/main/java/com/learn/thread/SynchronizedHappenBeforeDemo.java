package com.learn.thread;

/**
 *  演示 Synchronized 的可见性。以及普通变量可能不可见
 */
public class SynchronizedHappenBeforeDemo {

    /**
     * 普通成员变量，没有 volatile，也没有 synchronized。
     */
    private static boolean running = true;

    /**
     * 专门用于加锁的对象。
     */
    private static final Object LOCK = new Object();


    public static void main(String[] args) throws InterruptedException {

      //  VisibilityProblem();

       SynchronizedVisibility();



    }


    private static void SynchronizedVisibility() throws InterruptedException {

        Thread worker = new Thread(() -> {
            System.out.println("工作线程启动");

            while (true) {
                synchronized (LOCK) {
                    if (!running) {
                        break;
                    }
                }
            }

            System.out.println("工作线程检测到 running=false，结束运行");
        }, "worker");

        worker.start();

        Thread.sleep(1000);

        System.out.println("主线程准备将 running 修改为 false");

        synchronized (LOCK) {
            running = false;
        }

        System.out.println("主线程已经将 running 修改为 false");

        worker.join();

        System.out.println("主线程结束");


    }

    private static void VisibilityProblem() throws InterruptedException {
        Thread worker = new Thread(() -> {
            System.out.println("工作线程启动");
            while (running) {
                // 一直循环
            }

            System.out.println("工作线程检测到 running=false，结束运行");
        }, "worker");

        worker.start();

        // 让工作线程先运行一段时间
        Thread.sleep(1000);

        System.out.println("主线程准备将 running 修改为 false");

        running = false;

        System.out.println("主线程已经将 running 修改为 false");


    }
}
