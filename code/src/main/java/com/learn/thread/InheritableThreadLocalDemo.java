package com.learn.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 演示InheritableThreadLocal
 */
public class InheritableThreadLocalDemo {

    private static final ExecutorService executor =
            new ThreadPoolExecutor(
                    8,
                    16,
                    60L,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(200),
                    runnable -> {
                        Thread thread = new Thread(runnable);
                        thread.setName("product-query-" + thread.getId());
                        return thread;
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    private static final InheritableThreadLocal<String> LOCAL =
            new InheritableThreadLocal<>();


    public static void main(String[] args) {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;
        pool.prestartAllCoreThreads();

        ThreadLocalPoolInheritableThreadLocal();
    }

    private static void ThreadLocalPoolInheritableThreadLocal(){
        LOCAL.set("张三");

        executor.submit(() -> {

            System.out.println("通过池化线程能够获取父线程的InhertableThreadLocal"+LOCAL.get());
        });

    }


    // 演示父子线程可以通过InheritableThreadLocal获取传递参数
    private static void InheritableThreadLocal() {
        InheritableThreadLocal<String> local = new InheritableThreadLocal();
        local.set("张三");

        Thread t1 = new Thread(() -> {

            System.out.println("我是子线程，获取父线程的值");
            System.out.println("InheritableThreadLocal" + local.get());

        }, "t1");
        t1.start();
    }

    //演示 父子线程通过ThreadLocal无法获取参数，因为ThreadLocal是每个线程所有具有的字段。
    private static void ThreadLocalByChildrenThread() {
        ThreadLocal<String> local = new ThreadLocal();
        local.set("张三");

        Thread t1 = new Thread(() -> {

            System.out.println("我是子线程，获取父线程的值");
            System.out.println("ThreadLocal" + local.get());

        }, "t1");
        t1.start();

    }
}
