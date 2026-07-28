1. ForkJoinPool和ThreadPoolExecutor都是Java中常用的线程池的实现

2. 实现方式上：
- ForkJoinPool 是基于工作窃取（Work-Stealing）算法实现的线程池，ForkJoinPool 中每个线程都有自己的工作队列，用于存储待执行的任务。当一个线程执行完自己的任务之后，会从其他线程的工作队列中窃取任务执行，以此来实现任务的动态均衡和线程的利用率最大化。

- ThreadPoolExecutor 是基于任务分配（Task-Assignment）算法实现的线程池，ThreadPoolExecutor 中线程池中有一个共享的工作队列，所有任务都将提交到这个队列中。线程池中的线程会从队列中获取任务执行，如果队列为空，则线程会等待，直到队列中有任务为止。

