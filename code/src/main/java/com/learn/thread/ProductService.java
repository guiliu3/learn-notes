package com.learn.thread;

import java.util.concurrent.*;

public class ProductService {

    private final ExecutorService executor =
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

    public ProductDetail queryProductDetail(Long productId) {

        CompletableFuture<Product> productFuture =
                CompletableFuture.supplyAsync(
                        () -> queryProduct(productId),
                        executor
                );

        CompletableFuture<Stock> stockFuture =
                CompletableFuture.supplyAsync(
                        () -> queryStock(productId),
                        executor
                );

        CompletableFuture<Coupon> couponFuture =
                CompletableFuture.supplyAsync(
                        () -> queryCoupon(productId),
                        executor
                ).exceptionally(ex -> {
                    // 优惠查询失败时允许降级
                    return Coupon.empty();
                });

        CompletableFuture.allOf(
                productFuture,
                stockFuture,
                couponFuture
        ).join();

        return new ProductDetail(
                productFuture.join(),
                stockFuture.join(),
                couponFuture.join()
        );
    }

    private Product queryProduct(Long productId) {
        return new Product();
    }

    private Stock queryStock(Long productId) {
        return new Stock();
    }

    private Coupon queryCoupon(Long productId) {
        return new Coupon();
    }

    static class Product {
    }

    static class Stock {
    }

    static class Coupon {
        static Coupon empty() {
            return new Coupon();
        }
    }

    static class ProductDetail {
        ProductDetail(Product product, Stock stock, Coupon coupon) {
        }
    }
}