package com.learn.thread;

import java.util.concurrent.CompletableFuture;

/**
 * 演示CompletableFuture 实现线程同步
 */
public class CompletableFutureThreadExecute {

    public static void main(String[] args) {

        CompletableFuture<Void> future =CompletableFuture.runAsync(new MyThread4("T1"));
        future.join();

        CompletableFuture<Void> future2 =CompletableFuture.runAsync(new MyThread4("T2"));
        future2.join();


        CompletableFuture<Void> future3 =CompletableFuture.runAsync(new MyThread4("T3"));
        future3.join();


    }




}

class MyThread4 implements Runnable {
    private String name;

    public MyThread4(String name) {
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