package com.learn.thread;

import java.util.concurrent.*;

/**
 * 演示Callable与线程池的结合案例
 *
 *
 *
 */
public class FutureAndExecutorServiceExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Callable<String> callable = new Callable() {
            @Override
            public String call() throws Exception {
                System.out.println("Enter Callable ");
                Thread.sleep(1000L);
                return "  callable reture";
            }
        };

        System.out.println("开始提交线程池的任务");
        Future<String> submit = executorService.submit(callable);
        System.out.println("Do something else while callable is getting executed");
        System.out.println("Retrieved: " + submit.get());
        executorService.shutdown();
    }

}
