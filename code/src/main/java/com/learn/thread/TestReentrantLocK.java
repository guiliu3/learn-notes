package com.learn.thread;


import java.util.concurrent.locks.ReentrantLock;

/**
 * 演示ReentrantLocK的同步
 */
public class TestReentrantLocK {

    private static  final ReentrantLock lock = new  ReentrantLock();

    public static void main(String[] args) {

        Runnable task = () -> {
            String name = Thread.currentThread().getName();

            System.out.println(name + " -> 尝试获取锁");

            lock.lock();

            try {
                System.out.println(name + " -> 获取锁");

                Thread.sleep(3000);

                System.out.println(name + " -> 执行业务");

            } catch (Exception e) {
                e.printStackTrace();
            } finally {

                System.out.println(name + " -> 释放锁");

                lock.unlock();
            }
        };

        new Thread(task, "t1").start();
        new Thread(task, "t2").start();
        new Thread(task, "t3").start();
    }

}

