package com.learn.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 演示 CompletableFuture 异步并行执行
 */
public class CompletableFutureDemo {


    public static void main(String[] args) throws ExecutionException, InterruptedException {

//        // 查询产品信息
//        CompletableFuture<String> productFuture = CompletableFuture.supplyAsync(()->queryProduct());
//
//        // 查询 订单信息
//        CompletableFuture<Integer> stockFuture = CompletableFuture.supplyAsync(()->queryStock());
//
//        // 查询 优惠卷信息
//        CompletableFuture<String> couponFuture = CompletableFuture.supplyAsync(()->queryCoupon());
//
//        CompletableFuture<Void> allFuture = CompletableFuture.allOf(productFuture,stockFuture,couponFuture);
//
//        allFuture.join();
//
//        String s = productFuture.get();
//
//        System.out.println(s);
//
//        System.out.println(stockFuture.join());
//
//        System.out.println(couponFuture.join());

        CompletableFuture<Integer> future =
                CompletableFuture.supplyAsync(() -> 10)
                        .thenApply(value -> value * 2)
                        .thenApply(value -> value + 5);
        System.out.println(future.get());


    }

    private static String queryProduct() {
        return "iPhone";
    }

    private static Integer queryStock() {
        return 100;
    }

    private static String queryCoupon() {
        return "满1000减100";
    }

}
