package com.learn.thread;

import java.util.concurrent.CountDownLatch;

/**
 *  演示用CountDownLatch控制线程同步调用
 */
public class ThreadExcuteCountDownLatchDemo {

    public static void main(String[] args) throws InterruptedException {

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);


        // 创建并启动线程T1
        Thread t1 = new Thread(new MyThread1(latch1), "T1");
        t1.start();

        // 等待线程T1执行完
        latch1.await();

        // 创建并启动线程T1
        Thread t2 = new Thread(new MyThread1(latch2), "T2");
        t2.start();

        // 等待线程T2执行完
        latch2.await();

        // 创建并启动线程T1
        Thread t3 = new Thread(new MyThread1(latch3), "T2");
        t3.start();

        // 等待线程T3执行完
        latch3.await();


    }
}

class MyThread1 extends Thread{

    private CountDownLatch latch;

    public MyThread1(CountDownLatch latch){
        this.latch= latch;
    }

    @Override
    public void run(){

        try {
            Thread.sleep(1000L);
            System.out.println(Thread.currentThread().getName() + " is Running.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            // 完成一个线程。计数器-1
            latch.countDown();
        }

    }


}