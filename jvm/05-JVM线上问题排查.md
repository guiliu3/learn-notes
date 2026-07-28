# 05-JVM线上问题排查

## 1. 面试先给结论

JVM 线上排查不是背几个命令，而是要先判断问题类型：CPU 高、内存高、Full GC 频繁、线程阻塞、接口超时、服务假死。然后用合适的命令定位到进程、线程、堆、GC、锁和业务代码。

面试表达可以这样说：

> 我排查 JVM 线上问题一般先看现象和影响范围，比如是 CPU 飙高、内存打满、频繁 Full GC，还是线程阻塞导致接口超时。然后用 top、jps、jstack、jstat、jmap 等工具定位。CPU 高重点看高 CPU 线程栈；内存问题重点看 GC 和堆 dump；线程阻塞重点看线程状态和锁等待。最后结合业务日志、慢 SQL、线程池、连接池一起判断，不会只盯 JVM 本身。

## 2. 常见线上 JVM 问题

常见类型：

- CPU 100%。
- 内存占用过高。
- Full GC 频繁。
- 接口大量超时。
- 线程池打满。
- 死锁。
- 内存泄漏。
- 服务假死。
- OOM。

注意：

> JVM 问题经常只是结果，根因可能是慢 SQL、Redis 超时、下游接口慢、线程池参数不合理、缓存无限增长或代码死循环。

## 3. CPU 100% 怎么排查

### 3.1 排查流程

1. 用 `top` 找到高 CPU 进程。
2. 用 `top -H -p pid` 找到高 CPU 线程。
3. 把线程 ID 转成 16 进制。
4. 用 `jstack pid` 导出线程栈。
5. 根据 16 进制线程 ID 找到对应线程。
6. 看线程栈正在执行什么代码。
7. 用 `jstat` 判断是否频繁 GC。

常用命令：

```bash
top
top -H -p <pid>
printf "%x\n" <tid>
jstack <pid> > thread.log
jstat -gcutil <pid> 1000 10
```

### 3.2 常见原因

- 死循环。
- 大量自旋。
- 正则表达式回溯严重。
- JSON 序列化大对象。
- 频繁 Full GC。
- 大量线程上下文切换。
- 无索引 SQL 导致应用线程堆积。

面试表达：

> CPU 高时我会先定位到具体线程，而不是直接猜业务问题。通过 top -H 找线程，再用 jstack 看线程栈。如果线程在业务代码里循环，可能是死循环或自旋；如果大量线程在 GC，说明可能是内存压力；如果很多线程卡在数据库调用，要结合慢 SQL 和连接池排查。

## 4. Full GC 频繁怎么排查

### 4.1 先看 GC 情况

常用命令：

```bash
jstat -gcutil <pid> 1000 10
jstat -gccause <pid> 1000 10
```

关注：

- Young GC 次数和耗时。
- Full GC 次数和耗时。
- Old 区使用率。
- Metaspace 使用率。
- GC 原因。

### 4.2 典型现象

- Old 区持续上升，Full GC 后降不下来。
- Full GC 非常频繁。
- 每次 Full GC 暂停时间很长。
- 接口 RT 周期性抖动。

### 4.3 常见原因

- 内存泄漏。
- 大对象直接进入老年代。
- 缓存无上限。
- 静态集合持续增长。
- ThreadLocal 未清理。
- 一次性加载大量数据。
- 堆内存配置过小。
- 对象创建速度过快。

## 5. 内存泄漏怎么排查

### 5.1 导出堆 dump

命令：

```bash
jmap -dump:format=b,file=heap.hprof <pid>
```

也可以在 JVM 参数中配置 OOM 自动 dump：

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/dump
```

### 5.2 用 MAT 分析

重点看：

- Dominator Tree。
- Leak Suspects。
- 大对象。
- GC Roots。
- 哪些对象无法被回收。

常见泄漏点：

- 静态 Map/List 持续增长。
- 本地缓存没有淘汰策略。
- ThreadLocal 没有 remove。
- 监听器未注销。
- 连接、流、ResultSet 未关闭。
- 队列任务无限堆积。

面试表达：

> 内存泄漏我会先看 Full GC 后老年代是否降不下来。如果确认有泄漏，就导出 heap dump，用 MAT 看大对象和 GC Roots，判断对象为什么无法被回收。常见原因是静态集合、本地缓存、ThreadLocal、队列堆积。

## 6. 线程阻塞怎么排查

### 6.1 看线程状态

用：

```bash
jstack <pid>
```

常见状态：

- RUNNABLE：运行中或等待 CPU，也可能在 native IO。
- BLOCKED：等待锁。
- WAITING：无限期等待。
- TIMED_WAITING：限时等待。

### 6.2 常见阻塞点

- synchronized 锁竞争。
- ReentrantLock 未释放。
- 数据库连接池获取连接阻塞。
- HTTP 调用无超时。
- Redis 调用阻塞。
- 线程池队列堆积。
- CountDownLatch 等待不释放。

### 6.3 死锁

用 `jstack` 可以看到 Java-level deadlock。

死锁常见原因：

- 多个线程以不同顺序获取锁。
- 锁范围太大。
- 持锁时调用外部接口。

## 7. OOM 怎么排查

### 7.1 常见 OOM 类型

- Java heap space。
- GC overhead limit exceeded。
- Metaspace。
- Direct buffer memory。
- Unable to create new native thread。

### 7.2 排查思路

Java heap space：

- 看堆 dump。
- 找大对象和泄漏对象。

Metaspace：

- 看是否大量动态生成类。
- 例如 CGLIB、反射、脚本引擎。

Direct buffer memory：

- 看 NIO、Netty、文件传输。
- 堆外内存未释放。

Unable to create new native thread：

- 线程数过多。
- 线程池配置不合理。
- 系统线程资源不足。

## 8. 中考查询系统怎么讲

场景：

```text
成绩开放当天，大量请求超时，CPU 升高，线程池接近打满。
```

排查：

1. 看 Gateway/Sentinel 是否限流。
2. 看应用 CPU、内存、GC。
3. 用 jstack 看线程是否卡在 DB/Redis/锁。
4. 看 Tomcat 线程池、业务线程池队列。
5. 看 Redis 命中率是否下降。
6. 看数据库连接池和慢 SQL。

面试表达：

> 中考查询系统如果高峰期大量超时，我不会只看 JVM。先看请求是否被限流，再看应用线程是否都卡在 Redis 或数据库。如果 jstack 显示大量线程等待数据库连接，结合 Redis 命中率下降和 DB 慢 SQL，就说明 JVM 只是表现，根因可能是缓存穿透或慢 SQL。

## 9. 数据同步平台怎么讲

场景：

```text
Kafka Lag 上升，消费者处理变慢，应用线程池堆积。
```

排查：

1. 看消费者线程是否卡在目标库写入。
2. 看线程池队列和活跃线程数。
3. 看 GC 是否频繁。
4. 看目标库慢 SQL、锁等待、连接池。
5. 看是否补偿任务和实时消费抢资源。

面试表达：

> 数据同步平台如果 Lag 上升，我会用 jstack 看消费者线程卡在哪里。如果大量线程卡在 JDBC 写入或连接池获取连接，就说明瓶颈在数据库，不是 Kafka。处理上要限制补偿任务并发，优化目标库 SQL，避免盲目增加消费者线程。

## 10. 高频问题

### 10.1 CPU 100% 怎么排查

答法：

> 先 top 找高 CPU 进程，再 top -H -p pid 找高 CPU 线程，把线程 ID 转 16 进制，用 jstack 找到对应线程栈，看是业务死循环、自旋、频繁 GC 还是外部调用导致。再结合 jstat 看 GC。

### 10.2 Full GC 频繁怎么排查

答法：

> 用 jstat 看 GC 频率、耗时和 Old 区变化。如果 Full GC 后 Old 区降不下来，怀疑内存泄漏，导出 heap dump 用 MAT 分析大对象和 GC Roots。

### 10.3 线程大量 BLOCKED 怎么办

答法：

> 用 jstack 看阻塞在哪个锁上，找到持锁线程和等待线程。再看是否锁范围过大、持锁调用外部接口、锁顺序不一致或代码没有释放锁。

### 10.4 OOM 怎么排查

答法：

> 先看 OOM 类型。堆 OOM 导出 dump 用 MAT 分析；Metaspace 看动态类生成；Direct memory 看 NIO/Netty；unable to create thread 看线程数和线程池配置。

## 11. 你要背下来的 1 分钟版本

> JVM 线上排查要先分类。CPU 高就 top 找进程、top -H 找线程、jstack 定位代码，再用 jstat 看 GC。Full GC 频繁就看 Old 区是否持续上涨，必要时 jmap dump，用 MAT 分析大对象和 GC Roots。线程阻塞就看 jstack 里的 BLOCKED、WAITING，以及是否卡在锁、数据库连接池、Redis 或 HTTP 调用。排查时不能只看 JVM，还要结合线程池、连接池、慢 SQL、Redis 命中率和业务日志一起判断。

