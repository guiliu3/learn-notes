- 共同点：wait() 和wait(long) 和sleep(long)的效果都是让当前线程暂时放弃CPU的使用权，进入阻塞状态

- 不同点:
  1. 方法归属不同：sleep是Thread的静态方法，剩余线程，而wait是属于Obeject,它是等待资源，而不是线程。
  2. 醒来时机不同:
      - sleep(long)和wait(long)都会等待响应毫秒后醒来
      - wait方法可以通过notify唤醒。
      - sleep和wait都可以被打断唤醒
  3. 锁特性不同:
      - wait方法的调用必须要获取wait对象的锁，而Sleep没有限制
      - 执行wait方法，会释放锁的资源，别的线程可以尝试获取锁资源。
      - sleep方法，在synchronized中是不会释放锁资源，因为它只是休息一下

- 测试用例：
 src/main/java/com/learn/thread/WaitVsSleep.java