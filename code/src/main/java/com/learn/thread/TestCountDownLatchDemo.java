package com.learn.thread;

import java.util.concurrent.CountDownLatch;

/**
 * 演示CountDownLatch,主线程等待子线程全部结束，才继续执行
 */
public class TestCountDownLatchDemo {


    public static void main(String[] args) throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(3);

        Runnable task = ()->{

            String name = Thread.currentThread().getName();
            System.out.println(name + " 开始执行");

            try {
                Thread.sleep((long)(Math.random() * 3000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println(name + " 结束执行");
            countDownLatch.countDown();
        };

        new Thread(task,"t1").start();
        new Thread(task,"t2").start();
        new Thread(task,"t3").start();

        System.out.println("主线程等待...");
        countDownLatch.await();
        System.out.println("所有线程运行完毕...");

    }
}
