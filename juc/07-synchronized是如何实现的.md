# Synchronized锁的实现


- 同步方法：同步方法的常量池有一个ACC_SYNCHRONIZED标志，若访问同步方法时，需要判断是否这个标志，若有，则去获取监视器锁，然后执行方法，执行完成后，再释放监视器锁。持锁期间，其余线程访问同步方法，会被阻塞。
- 源码：com.learn.thread.SynchronizedBytecodeDemo.syncMethod
```text
 public synchronized void syncMethod();
    descriptor: ()V
    flags: ACC_PUBLIC, ACC_SYNCHRONIZED
    Code:
      stack=2, locals=1, args_size=1
         0: getstatic     #4                  // Field java/lang/System.out:Ljava/io/PrintStream;
         3: ldc           #5                  // String syncMethod
         5: invokevirtual #6                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
         8: return
      LineNumberTable:
        line 12: 0
        line 13: 8

```
- 同步代码块：同步代码块通过monitorenter和monitorexit指令完成的，每一个对象维护一个被锁次数。
- 源码：com.learn.thread.SynchronizedBytecodeDemo.syncBlock
```text
  public void syncBlock();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=2, locals=3, args_size=1
         0: aload_0
         1: getfield      #3                  // Field lock:Ljava/lang/Object;
         4: dup
         5: astore_1
         6: monitorenter
         7: getstatic     #4                  // Field java/lang/System.out:Ljava/io/PrintStream;
        10: ldc           #8                  // String syncBlock
        12: invokevirtual #6                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
        15: aload_1
        16: monitorexit
        17: goto          25
        20: astore_2
        21: aload_1
        22: monitorexit
        23: aload_2
        24: athrow
        25: return
      Exception table:
         from    to  target type
             7    17    20   any
            20    23    20   any
      LineNumberTable:
        line 28: 0
        line 29: 7
        line 30: 15
        line 31: 25
      StackMapTable: number_of_entries = 2
        frame_type = 255 /* full_frame */
          offset_delta = 20
          locals = [ class com/learn/thread/SynchronizedBytecodeDemo, class java/lang/Object ]
          stack = [ class java/lang/Throwable ]
        frame_type = 250 /* chop */
          offset_delta = 4

```

## Monitor
在 Java 虚拟机(HotSpot)中，Monitor 是基于 C++ 实现的，由 ObjectMonitor 实现的

- 数据结构
```text
ObjectMonitor() {
    _header       = NULL;
    _count        = 0;
    _waiters      = 0,
    _recursions   = 0;
    _object       = NULL;
    _owner        = NULL;
    _WaitSet      = NULL;
    _WaitSetLock  = 0 ;
    _Responsible  = NULL ;
    _succ         = NULL ;
    _cxq          = NULL ;
    FreeNext      = NULL ;
    _EntryList    = NULL ;
    _SpinFreq     = 0 ;
    _SpinClock    = 0 ;
    OwnerIsThread = 0 ;
}

```
- _owner：指向持有 ObjectMonitor 对象的线程
- _WaitSet：存放处于 wait 状态的线程队列
- _EntryList：存放处于等待锁 block 状态的线程队列
- _recursions：锁的重入次数
- _count：用来记录该线程获取锁的次数

### 流程说明：
> - 当多个线程同时访问同一段同步代码时，首先会进入_EntiryList队列中，当某一个线程获取到monitor时，_owner会设置当前线程，即获取对象锁
> - 若持有锁的线程调用了wait方法时，该线程会进入_WaitSet集合等待被唤醒。同时释放锁。

## Synchronized锁的什么
 - Synchronized最终锁的都是对象，synchronized的普通方法，其实锁的是具体调用这个方法的实例对象，而synchronized的静态方法，其实锁的是这个方法锁属于的类对象。

## synchronized是如何保证原子性、可见性、有序性的？
> - 原子性：是指一个操作是不可中断的。
> - 原子性：它是通过monitorenter和monitorexit 对Monitor加锁和释放锁。保证原子性，它不是让每一条指令变成原子性，而是让同步的代码块不能其他线程干扰。
> - 有序性：
> - 可见性：java内存模型规定，对一个锁的解锁，会先行发生于对一个锁的加锁。
>   1. 线程释放锁前，需要把共享变量的修改对其他线程公开；
>   2. 线程获得锁后，不能一直使用锁释放之前的旧数据；
>   3. 必须能够观察到之前持锁线程已经完成的修改。
>   4. 参考源码：com.learn.thread.SynchronizedHappenBeforeDemo
>   说明：对一个变量的解锁之前，必须把此变量同步到主存中去，这样解锁后，后续线程就可以获取到被修改后的值。

## synchronized的锁升级过程是怎样的
 - JDK 1.6之前的锁是重量级锁，后来进行了优化，引入了轻量级锁、偏向锁 
 - JDK 所以锁的状态分为4个，无锁、轻量级锁状态、偏向锁状态、重量级锁状态。
 - synchronized的才有这4个状态，是JVM为了优化Monitor而做的锁升级。
 
### 为什么需要锁升级
> 因为原先的重量级锁，需要去创建Monitor，然后线程阻塞和唤醒，都是需要用户态和内核态切换，没导致开销很大，所以想出来了不用Monitor可不可以实现。

### 锁升级的过程
 - 无锁状态：刚创建对象时，没有任何锁，对象头Mark Word 001（无锁）
 - 偏向锁：线程A第一次进入同步块时，对象头直接写入线程id以及将锁状态调为偏向锁。下次线程A再次进入时，不需要创建monitor ，不需要抢锁，速度快。
 - 轻量级锁：若第二个线程竞争，偏向锁失效，锁升级为轻量级，不是立即阻塞，而是CAS+自旋的方式不断尝试获取锁。
 - 重量级锁：自旋多次失败，竞争激烈，升级重量级锁，对象头指向一个重量级锁（Minotr），若一个线程想要获取锁，则需要进入等待队列。


## 自旋与阻塞的区别
 自旋饿坏阻塞的最大区别是：是否放弃CPU时间，阻塞会放弃CPU时间进入等待区，而自旋不会放弃，时刻检查资源是否可以被访问。





 
 









