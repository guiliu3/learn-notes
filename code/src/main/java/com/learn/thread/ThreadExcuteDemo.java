package com.learn.thread;

/**
 * 线程顺序执行
 */
public class ThreadExcuteDemo {


    public static void main(String[] args) {
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Thread 1 running");
            }
        });

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    thread1.join();
                } catch (InterruptedException e) {
                    System.out.println("join thread1 failed");
                }
                System.out.println("Thread 2 running");
            }
        });

        Thread thread3 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    thread2.join();
                } catch (InterruptedException e) {
                    System.out.println("join thread2 failed");
                }
                System.out.println("Thread 3 running");
            }
        });

        thread1.start();
        thread2.start();
        thread3.start();
    }

}
