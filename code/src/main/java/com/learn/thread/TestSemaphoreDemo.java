package com.learn.thread;

import java.util.concurrent.Semaphore;

/**
 * 演示Semaphore同时只有2个线程可以访问共享资源
 */
public class TestSemaphoreDemo {

    static final Semaphore semaphore = new Semaphore(2);

    public static void main(String[] args) {

        Runnable task = ()->{
            String name = Thread.currentThread().getName();
            try {
                System.out.println("尝试获取锁资源"+name);
                semaphore.acquire();
                System.out.println(name + " 获取许可证");
                Thread.sleep(3000);
                System.out.println(name + " 执行业务");

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }finally {
                System.out.println(name + " 释放许可证");
                semaphore.release();
            }

        };

        for (int i = 1; i <= 5; i++) {
            new Thread(task, "t" + i).start();
        }

    }
}
