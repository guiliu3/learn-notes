# CountDownLatch、CyclicBarrier、Semaphore区别？
 3者都是juc的同步辅助类 

- CountDownLatch:一个计数器，一个线程等待很多个线程多完成操作后，才能继续操作
- CyclicBarrier:实现多个线程在同一个屏障等待，然后都到了这个屏障，才继续一起执行
- Semapore：一个计数号量，它允许多个线程获取资源，以许可证的形式去，若许可证还有，则允许新线程继续执行，可以用与控制资源数量访问。

