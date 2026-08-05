# 同步队列

同步队列主要用于实现锁的获取和释放。如我们常用的ReentrantLock，就是基于同步队列来实现的。

- 概念：
我们在介绍AQS的时候介绍过，它是一个FIFO队列，节点类型为AQS内部的Node类。当一个线程尝试获取锁失败时，它会被封装成一个Node节点加入到队列的尾部（每个节点（Node）代表一个等待的线程）。当锁被释放时，头节点的线程会被唤醒，尝试再次获取锁。

```java
static final class Node {
    // 前驱和后继节点，构成双向链表
    Node prev;
    Node next;
    // 线程本身
    Thread thread;
    // 状态信息，表示节点在同步队列中的等待状态
    int waitStatus;
    // ...
}

```

- 队列操作：
  1. 当一个线程尝试获取锁并失败时，AQS会将该线程包装成一个节点（Node）并加入到队列的尾部。

  2. 当锁被释放时，头节点（持有锁的线程）会通知其后继节点（如果存在的话），后继节点尝试获取锁。 这个过程会一直持续，直到有线程成功获取锁或者队列为空。



# 条件队列
 
- 概念：   
 条件队列用于实现条件变量，允许线程在特定条件不满足时挂起，直到其他线程改变了条件并显式唤醒等待在该条件上的线程。比较典型的一个条件队列的使用场景就是ReentrantLock的Condition。

- 概念：
  条件队列与同步队列不同，它是基于Condition接口实现的，用于管理那些因为某些条件未满足而等待的线程。当条件满足时，这些线程可以被唤醒。每个Condition对象都有自己的一个条件队列。 


- 结构
```java
public class ConditionObject implements Condition, java.io.Serializable {
    // 条件队列的首尾节点
    private transient Node firstWaiter;
    private transient Node lastWaiter;
    // ...
}

```


## AQS的独占模式和共享模式

- 独占模式:意味着一次只有一个线程可以获取同步状态。这种模式通常用于实现互斥锁，如ReentrantLock
- 共享模式:允许多个线程同时获取同步状态。这种模式通常用于实现如信号量（Semaphore）和读写锁（ReadWriteLock的读锁）等同步组件。


当需要保证某个资源或一段代码在同一时间内只能被一个线程访问时，独占模式是最合适的选择。

当资源或数据主要被多个线程读取，而写操作相对较少时，共享模式能够提高并发性能。如我们经常使用的Semaphore和CountDownLatch，用来多个线程控制共享资源的    


