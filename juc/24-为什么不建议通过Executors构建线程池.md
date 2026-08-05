# 为什么不建议通过Executors构建线程池
 Executors创建线程池可能会导致OOM(OutOfMemory ,内存溢出)
 
- 参考演示出现OOM的案例：com.learn.thread.ExecutorsDemo
- 因为默认使用的LinkedBlockingQueue
```java
   public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>());
    }

```

```java
    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

```

>LinkedBlockingQueue是一个用链表实现的有界阻塞队列，容量可以选择进行设置，不设置的话，将是一个无边界的阻塞队列，最大长度为Integer.MAX_VALUE。

>注意：所以最好自定线程池，并设置好队列容量大小。


