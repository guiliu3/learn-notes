package com.learn.thread;

import java.util.concurrent.CompletableFuture;

/**
 *  演示CompletableFuture常用的方法
 */
public class CompletableFutureAllDemo {


    public static void main(String[] args) throws InterruptedException {


    //    runAsync();

      //  supplyAsync();

//        thenApply();
//        thenAccept();
//        thenRun();
//        thenCompose();
//        thenCombine();
        exceptionally();
        System.out.println("主线程结束");
    }



    // 无返回的任务执行，相比于： 发送短信、记录日志、异步通知
  private static void runAsync(){

      CompletableFuture future = CompletableFuture.runAsync(()->{
          System.out.println("发送短信，一个无返回的任务执行");
          try {
              Thread.sleep(1000L);
          } catch (InterruptedException e) {
              throw new RuntimeException(e);
          }

      });
  }

    // 有返回的任务执行
    private static void supplyAsync() throws InterruptedException {

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return "我具有返回值的任务";
        });
        String join = future.join();
        Thread.sleep(1000L);
        System.out.println("supplyAsync f返回值" + join);
    }


    // 接收结果返回新结果
    private static void thenApply(){
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(()->10).thenApply(value->value*2);
        Integer join = future.join();
        System.out.println("thenApply 接收结果返回旧结果： "+join);
    }

    // 只接受结果，不返回结果
    private static void thenAccept() {
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> 10).thenAccept(value -> {
            System.out.println("then accept 内部输出："+value);
        });
    }

    // thenRun 不接收结果，也不返回结果，只跑
    private static void thenRun(){
        CompletableFuture<Void> future =
                CompletableFuture.supplyAsync(() -> "查询结果")
                        .thenRun(() -> {
                            System.out.println("任务执行结束");
                        });

    }

    // thenCompose ,串行依赖
    private static void thenCompose() {
        CompletableFuture<Order> future = CompletableFuture.supplyAsync(() -> queryUser()).thenCompose(
                user -> {
                    return CompletableFuture.supplyAsync(()->queryOrder(user.getId()));
                }
        );

        Order join = future.join();
        System.out.println(join.getName());
    }

    private static void thenCombine(){
        CompletableFuture<String> userFuture =
                CompletableFuture.supplyAsync(() -> "张三");

        CompletableFuture<Integer> orderFuture =
                CompletableFuture.supplyAsync(() -> 10);

        CompletableFuture<String> resultFuture =
                userFuture.thenCombine(
                        orderFuture,
                        (user, orderCount) ->
                                user + "共有" + orderCount + "个订单"
                );

        System.out.println(resultFuture.join());
    }

    private static void exceptionally(){

        CompletableFuture<String> future = CompletableFuture.supplyAsync(()->{
            int value =1/0;
            return "成功";
        }).exceptionally(ex->{
            System.out.println("发生了异常");
            return "默认结果";
        });
        System.out.println(future.join());
    }

    private static Order queryOrder(Integer id) {
        Order order = new Order();
        order.setName("123");
        return order;
    }

    private static User queryUser() {
        User user = new User();
        user.setId(1);
        return user;
    }


}

class User {

    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}

class Order {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String id) {
        this.name = id;
    }
}
