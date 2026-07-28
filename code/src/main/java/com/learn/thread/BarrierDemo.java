package com.learn.thread;

import java.util.concurrent.CyclicBarrier;

/**
 *  Barrier演示
 */
public class BarrierDemo {

    static CyclicBarrier barrier = new CyclicBarrier(3);

    public static void main(String[] args) {

        Runnable task = () -> {

            String name = Thread.currentThread().getName();

            try {

                System.out.println(name + " 到达集合点");

                Thread.sleep((long) (Math.random() * 3000));

                System.out.println(name + " 等待其他人");

                barrier.await();

                System.out.println(name + " 一起出发");

            } catch (Exception e) {
                e.printStackTrace();
            }

        };

        new Thread(task, "t1").start();
        new Thread(task, "t2").start();
        new Thread(task, "t3").start();
    }


}
