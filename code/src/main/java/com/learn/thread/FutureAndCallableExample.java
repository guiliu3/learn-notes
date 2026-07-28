package com.learn.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * 演示Callable有返回的线程创建以及如何通过FutureTask获取线程执行的结果
 *
 *
 *
 */
public class FutureAndCallableExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        Callable<String> callable = new Callable() {
            @Override
            public String call() throws Exception {
                System.out.println("Enter Callable ");
                Thread.sleep(1000L);
                return "  callable reture";
            }
        };
        FutureTask<String> futureTask = new FutureTask<>(callable);
        Thread thread = new Thread(futureTask);
        thread.start();

        System.out.println("Do something else while callable is getting executed");
        System.out.println("Retrieved"+futureTask.get());


    }
}
