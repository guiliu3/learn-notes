# synchronized和reentrantLock区别

 synchronized和ReentrantLock都是同步控制，但是ReentrantLock功能丰富一些
 
- Synchronized 是基于C++实现，而ReentrantLock是jdk实现的
- Synchronized可以自动获取锁和释放，而ReentrantLock是需要手动获取/手动释放锁
- ReentrantLock具有响应中断，超时等待特性
- ReenttrantLock可以设置公平和非公平锁，而synchronized是非公平锁。

## synchronized为什么是非公平锁
> 虽然synchronized这个底层是Monitor有的等待队列
 ```text
        ObjectMonitor
        ┌────────────────────┐
Owner──►│ owner              │
        │                    │
        │ EntryList          │◄──── 等待获取锁的线程
        │   T2               │
        │   T3               │
        │   T4               │
        │                    │
        │ WaitSet            │◄──── wait() 的线程
        └────────────────────┘ 

 ``` 
>  EntryList是队列，但不等于按顺序取执行，因为它本身只是唤醒，而不是变成Running,而是Runable。
> 如果在锁释放的瞬间，一个线程去抢锁，也是可以在队列之前去执行的，可以直接拿到锁。
> 因为这么设计，使用非公平锁，性能好点。


## ReentrantLock的用法
  
- 使用：
  ```text
  public class Counter {
	  private final Lock lock = new ReentrantLock();
	  private int count;
	  public void add(int n) {
		 lock.lock();
		 try {
			count += n;
		 } finally {
			lock.unlock();
		 }
     }
  }
  ```
- 默认是非公平锁
  - 获取锁流程：
    1. sync.lock();
       ```java
         public void lock() {
              sync.lock();
          }
       ```
    2. 流程以非公平锁为例 NonfairSync.class
      ```java
          final void lock() {
             if (compareAndSetState(0, 1))
                 setExclusiveOwnerThread(Thread.currentThread());
             else
                 acquire(1);
         }
      ```
       若CAS成功获取到锁，则将 exclusiveOwnerThread = 当前线程，独占模式拥有锁的线程
      
       若CAS失败，则去执行acquire方法

    3. 执行 AQS.acquire方法
     ```java
         public final void acquire(int arg) {
            if (!tryAcquire(arg) &&
               acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
                selfInterrupt();
         }
   
     ```
    4. 执行 Sync.nonfairTryAcquire
      ```java
        final boolean nonfairTryAcquire(int acquires) {
             final Thread current = Thread.currentThread();
             int c = getState();
             if (c == 0) {
                 if (compareAndSetState(0, acquires)) {
                     setExclusiveOwnerThread(current);
                     return true;
                 }
             }
             else if (current == getExclusiveOwnerThread()) {
                 int nextc = c + acquires;
                 if (nextc < 0) // overflow
                     throw new Error("Maximum lock count exceeded");
                 setState(nextc);
                 return true;
             }
             return false;
         }
     ```     
     主要做了2个事情，第一个没人持锁，然后CAS成功，跟上面一样，设置当前线程。  若是当前自己线程，则支持重入，并state++
 
    5. 若获取锁失败，则执行addWaiter(Node.EXCLUSIVE);AQS下的addWaiter方法
       ```java
        private Node addWaiter(Node mode) {
            Node node = new Node(Thread.currentThread(), mode);
            // Try the fast path of enq; backup to full enq on failure
            Node pred = tail;
            if (pred != null) {
                node.prev = pred;
                if (compareAndSetTail(pred, node)) {
                    pred.next = node;
                    return node;
                }
            }
            enq(node);
            return node;
         }        
       ```
      把当前线程新建一个Node，放入到CLH的队尾，但是还没park

   6. 执行acquireQueued方法，AQS的方法
       ```java
          final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
       }   
       ```
     无线循环，一直去判断是否是head后面的节点， 若是则尝试拿锁结束，否则就park。

     疑问：那所有想拿锁的线程不是都在循环。      

- 释放锁：
   1. sync.执行release(1) 
      ```java
         public void unlock() {
             sync.release(1);
         }
      ```
   2. AQS.release方法
      ```java
         public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
           return false;
        } 
      ```
    流程就是：释放锁，然后唤醒下一个。  
   
  3. tryRelease方法
      ```java
           protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }
      ```
     若不是自己的线程持有锁，会异常，若是，则进行state-1，若重入锁，需要一直释放锁，等state=0，则free=true.

- 流程图
```text
                lock()

                   │
                   ▼
          CAS(state,0,1)

         ┌────────┴────────┐
         │                 │
      成功               失败
         │                 │
         ▼                 ▼
   Owner=当前线程      tryAcquire()
                           │
                           ▼
                    addWaiter()
                           │
                           ▼
                   加入CLH队列
                           │
                           ▼
                        park()
                           │
                    被unpark唤醒
                           │
                           ▼
                      tryAcquire()
                           │
                           ▼
                       获取锁成功


==============================

              unlock()

                  │
                  ▼
            state--

        state==0 ?

        ┌─────┴─────┐
        │           │
       否          是
        │           │
        ▼           ▼
     直接返回   Owner=null
                    │
                    ▼
           unparkSuccessor()
                    │
                    ▼
             唤醒head.next
                    │
                    ▼
          head.next再次CAS抢锁

```


