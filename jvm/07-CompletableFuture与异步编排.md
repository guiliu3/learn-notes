# 07-CompletableFuture与异步编排

## 1. 面试先给结论

`CompletableFuture` 是 Java 8 提供的异步编排工具，适合把多个独立任务并行执行、组合结果、统一处理异常。它能提高接口响应速度，但必须配合自定义线程池、超时控制和异常处理，否则容易把公共线程池拖垮。

面试表达可以这样说：

> CompletableFuture 适合做异步任务编排，比如多个互不依赖的查询可以并行执行，最后聚合结果。使用时我会指定自定义线程池，不直接依赖默认 ForkJoinPool；同时设置超时和异常兜底，避免某个任务卡住影响整体结果。

## 2. 为什么需要 CompletableFuture

传统串行调用：

```text
查询用户信息 100ms
查询订单信息 200ms
查询积分信息 150ms
总耗时约 450ms
```

异步并行：

```text
三个任务同时执行
总耗时约 max(100, 200, 150) = 200ms
```

适合：

- 多个独立远程调用。
- 多个独立数据库查询。
- 聚合接口。
- 异步通知。
- 批量任务拆分。

不适合：

- 强依赖顺序流程。
- 数据库已经是瓶颈。
- 下游承载能力不足。
- 没有超时和异常兜底的核心链路。

## 3. 常用方法

### 3.1 supplyAsync

有返回值异步任务：

```java
CompletableFuture.supplyAsync(() -> queryUser(), executor);
```

### 3.2 runAsync

无返回值异步任务：

```java
CompletableFuture.runAsync(() -> sendLog(), executor);
```

### 3.3 thenApply

对结果做转换：

```java
future.thenApply(user -> buildDTO(user));
```

### 3.4 thenCompose

串行依赖异步任务：

```java
future.thenCompose(user -> queryOrder(user.getId()));
```

### 3.5 thenCombine

合并两个独立任务结果：

```java
userFuture.thenCombine(orderFuture, (user, order) -> buildVO(user, order));
```

### 3.6 allOf

等待多个任务全部完成：

```java
CompletableFuture.allOf(f1, f2, f3).join();
```

### 3.7 exceptionally / handle

异常处理：

```java
future.exceptionally(ex -> defaultValue);
```

或：

```java
future.handle((result, ex) -> {
    if (ex != null) {
        return defaultValue;
    }
    return result;
});
```

## 4. 一定要指定自定义线程池

如果不指定线程池，默认使用 `ForkJoinPool.commonPool()`。

风险：

- 多个业务共享公共线程池。
- 阻塞 IO 任务会占满公共线程。
- 排查困难。
- 影响其他异步任务。

建议：

```java
CompletableFuture.supplyAsync(task, bizExecutor);
```

面试表达：

> 我不会在生产里让 CompletableFuture 默认使用 commonPool，尤其是包含 DB、Redis、HTTP 调用的 IO 任务。一般会按业务定义独立线程池，并设置有界队列和拒绝策略。

## 5. 异常处理

异步任务中的异常不会像同步代码一样直接抛到主线程。

必须显式处理：

- exceptionally。
- handle。
- whenComplete。
- join/get 捕获 CompletionException。

常见问题：

> 子任务异常没有处理，主流程 join 时才暴露，或者异常被吞掉导致结果不完整。

建议：

- 每个关键子任务都有异常兜底。
- 记录异常日志。
- 非核心任务失败不影响主流程。
- 核心任务失败要终止或返回明确错误。

## 6. 超时控制

Java 9 后有：

```java
orTimeout
completeOnTimeout
```

思路：

- 单个子任务设置超时。
- 总任务设置超时。
- 超时后返回默认值或失败。

注意：

> 超时返回不代表底层任务一定被取消，线程可能仍在执行，所以还要控制下游调用本身的超时时间。

## 7. join 和 get 区别

`get`：

- 抛受检异常。
- 需要显式 catch。

`join`：

- 抛运行时异常 CompletionException。
- 写法更简洁。

两者都会等待结果。

## 8. 中考查询系统怎么讲

场景：

- 查询成绩。
- 查询学校信息。
- 查询公告。
- 查询考生基础信息。

如果这些查询互不依赖，可以并行。

但要注意：

- 高峰期不要无限并行。
- 线程池要独立。
- Redis/DB 已经慢时，并行可能放大压力。
- 必须有超时和降级。

面试表达：

> 中考查询系统中，如果一个聚合接口需要查成绩、学校、公告等互不依赖的数据，可以用 CompletableFuture 并行查询，降低接口总耗时。但高峰期要控制线程池和下游 DB/Redis 连接数，不能为了并行把下游打满。对于公告这类非核心数据，失败可以降级为空；成绩查询是核心数据，失败要返回明确提示。

## 9. 数据同步平台怎么讲

场景：

- 同步一条主数据时，还要同步附件、子表、日志。
- 某些后置通知可以异步。
- 多个非强依赖任务可以并行。

注意：

- 同一事务内不要随便异步。
- ThreadLocal 不会自动跨线程传递。
- 异步任务失败要记录。
- 不要破坏业务顺序和一致性。

面试表达：

> 数据同步平台里，CompletableFuture 可以用于非核心后置任务，比如告警通知、日志增强、部分互不依赖的数据查询。但如果涉及同一个事务里的多表写入，我不会随便异步，因为事务上下文和 ThreadLocal 不会自动跨线程传递，异常处理和回滚边界也会变复杂。

## 10. 常见坑

### 10.1 默认线程池

不指定 executor，使用 commonPool，容易被阻塞任务占满。

### 10.2 忘记处理异常

子任务失败，主流程结果异常或被吞。

### 10.3 没有超时

某个子任务卡住，整个聚合接口卡住。

### 10.4 ThreadLocal 丢失

异步线程无法自动拿到主线程 ThreadLocal。

例如：

- 用户上下文。
- traceId。
- 数据源标记。

需要显式传参或使用上下文包装器。

### 10.5 事务失效

异步任务不在原事务线程内，不能默认认为共享同一个事务。

## 11. 高频问题

### 11.1 CompletableFuture 适合什么场景

答法：

> 适合多个互不依赖任务并行执行并聚合结果，比如聚合查询、远程调用组合、非核心异步通知。不适合强顺序依赖或下游资源已经成为瓶颈的场景。

### 11.2 为什么要指定线程池

答法：

> 不指定会使用 ForkJoinPool.commonPool，多个业务共享，阻塞 IO 任务可能把公共线程池占满。生产应使用自定义线程池，便于隔离、监控和控制队列。

### 11.3 异步任务异常怎么处理

答法：

> 可以用 exceptionally、handle、whenComplete 处理异常。核心任务异常要返回明确失败，非核心任务可以降级兜底，同时记录日志和告警。

### 11.4 CompletableFuture 会自动传递 ThreadLocal 吗

答法：

> 不会。ThreadLocal 是线程本地变量，异步任务切换线程后拿不到主线程上下文。需要显式传参，或使用包装后的线程池传递上下文。

### 11.5 异步一定能提升性能吗

答法：

> 不一定。如果任务互不依赖且下游资源足够，异步并行可以降低总耗时；但如果瓶颈在数据库或下游接口，增加并行度可能放大压力，导致整体更慢。

## 12. 你要背下来的 1 分钟版本

> CompletableFuture 适合做异步编排，把多个互不依赖任务并行执行后聚合结果。生产使用时必须指定自定义线程池，不能默认用 commonPool；还要设置超时和异常兜底，避免某个子任务卡住拖慢整体。它不会自动传递 ThreadLocal，也不会共享原事务上下文，所以涉及用户上下文、traceId、数据源标记或事务的场景要特别注意。异步不是越多越好，如果下游数据库已经是瓶颈，并行反而会放大压力。

