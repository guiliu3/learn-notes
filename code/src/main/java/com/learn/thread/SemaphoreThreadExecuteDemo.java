package com.learn.thread;

import java.util.concurrent.Semaphore;

/**
 * 借助Semaphore 实现线程同步
 */
public class SemaphoreThreadExecuteDemo {

    public static void main(String[] args) throws InterruptedException {


        // 创建Semaphore对象，用来做线程通信
        Semaphore semaphore = new Semaphore(1);

        // 等待线程T1执行完
        semaphore.acquire();

        // 创建并启动线程T1
        Thread t1 = new Thread(new MyThread2(semaphore), "T1");
        t1.start();

        // 等待线程T2执行完
        semaphore.acquire();

        // 创建并启动线程T2
        Thread t2 = new Thread(new MyThread2(semaphore), "T2");
        t2.start();


        // 等待线程T3执行完
        semaphore.acquire();

        // 创建并启动线程T3
        Thread t3 = new Thread(new MyThread2(semaphore), "T3");
        t3.start();


    }

}

class MyThread2 implements Runnable {
    private Semaphore semaphore;

    public MyThread2(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        try {
            // 模拟执行任务
            Thread.sleep(1000);
            System.out.println(Thread.currentThread().getName() + " is Running.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 释放许可证，表示完成一个线程
            semaphore.release();
        }
    }
}
