package com.learn.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 演示固定线程池实现线程同步
 */
public class ThreadPoolThreadExecuteDemo {

    public static void main(String[] args) {

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.submit(new MyThread3("T1"));

        executorService.submit(new MyThread3("T2"));

        executorService.submit(new MyThread3("T2"));

        executorService.shutdown();
    }


}

class MyThread3 implements Runnable {
    private String name;

    public MyThread3(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        try {
            // 模拟执行任务
            Thread.sleep(1000);
            System.out.println(name + " is Running.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}