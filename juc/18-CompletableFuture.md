# CompletableFuture的底层原理

- 演示源码：com.learn.thread.CompletableFutureDemo
- CompletableFuture 可以分为四个部分理解
  1. 任务结果容器
  2. 异步任务
  3. 回调链表
  4. 线程池调度

## CompletableFuture 本身保存任务结果
 volatile Object result; 
 
- 任务没有完成时：null
- 任务正常完成后：实际返回的值
- 任务异常后：包装后的异常对象


## 回调任务保存在一个栈结构中

volatile Completion stack;

- CompletableFuture 内部会把后续操作封装成一个个 Completion 节点。

## 前一个任务完成后，触发后续回调
- 例如：
```java
CompletableFuture.supplyAsync(() -> 10)
        .thenApply(value -> value * 2);
```

```text
1. 创建第一个 CompletableFuture
2. 创建异步任务
3. 将异步任务提交到线程池
4. thenApply 创建第二个 CompletableFuture
5. 将 thenApply 回调挂到第一个 Future 上
6. 第一个任务执行完毕，result = 10
7. 扫描并触发依赖它的 Completion 节点
8. 执行 value * 2
9. 第二个 Future 的 result = 20

```
任务完成时，不只是保存结果，还会主动触发依赖它的后续任务。

## 默认使用 ForkJoinPool

```text
private static final Executor asyncPool = useCommonPool ?
        ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();
```
在实际项目中，不推荐直接依赖公共线程池，最好使用自定义线程池。

```text
ExecutorService executor = new ThreadPoolExecutor(
        4,
        8,
        60,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(100),
        new ThreadPoolExecutor.CallerRunsPolicy()
);

CompletableFuture<String> future =
        CompletableFuture.supplyAsync(() -> "结果", executor);
```

ForkJoinPool.commonPool所有权是JVM全局共享的，且业务隔离较差。


## 同步方法和异步方法的区别
- thenApply：后续任务通常由“完成前一个任务的线程”直接执行。
- thenApplyAsync：重新交给线程池执行
- allOf：等待所有任务完成

## 异常处理
- exceptionally
- 发生异常时，提供降级结果
- 参考源码：com.learn.thread.CompletableFutureAllDemo.exceptionally
- handle 无论成功和失败都会追着你。并且返回新结果


- 参考源码：com.learn.thread.CompletableFutureAllDemo


## 项目推荐写法
- 参考源码：com.learn.thread.ProductService


## Completion 阶段
是链式的，那应该是 链表


