## happens-before(先行发生原则)
 解决多线程之间的可见性和有序性
- 官方定义： 如果操作A happens-before 操作B，那么A的执行结果对B是可见的，且A的执行顺序是在B之前。

## 一、为什么需要happens-before
- CPU执行不是直接操作主存的，而是各自的缓存

 ```
   int count =0 ; 初始化
   线程1  count= 100;
   线程2: 读取count值
   
           主内存

          count=0

          ↑     ↑

     CPU1缓存  CPU2缓存

     count=100 count=0

   可能线程1只是修改了缓存，而不是主存

 ```
 演示多线程下无序的问题,这里并不是指各自线程的调度顺序，而是线程内部的指令顺序

一个线程内部的代码执行顺序，可能被编译器、CPU指令重排序改变。
```text
  // 初始化变量
  boolean flag=false;
  int value=0;  

  线程1：
   value=100;
   flag=true;

  线程2：
    if(flag){

    System.out.println(value);

  }
  直觉是：线程2输出一定是100
  但是在没有happens-before可能出现 flag=true,vlaue还是0，因为存在指令重排。
  
  线程1的执行先后顺序是：  
     flag = true;
     value =100;  


```

- 产生2个问题
  1. 可见性 ：一个线程修改了，其余线程什么时候可以看到
  2. 有序性：happens-before定义了那些操作之间一ID给你存在可见性和顺序关系

    
## 二、 synchronized 和 happens-before
  对一个锁定解锁happens-before 后续这个锁的加锁

- 流程如图：
 ```text
    线程1:

  lock.lock()

   执行操作A

   unlock()


        happens-before


   线程2:

   lock.lock()

   执行操作B

 ```
线程1对释放锁之前的操作对线程2获取锁后的操作B一定是可见的。

- 可见性：
synchronized 通过 monitor 的加锁和释放锁建立 happens-before 规则。线程释放锁之前对共享变量的修改，在后续线程获取同一个锁之后一定可见

- 原子性：同一个时间只有一个线程执行
- 有序性：synchronized不是禁止重排序，而是保证了锁住的边界，其他线程看不到。


# volatile 和 happens-before
对一个 volatile 变量的写操作 happens-before 后续对这个 volatile 变量的读操作。

- 可见性：一个线程修改变量，其他线程马上知道。

> volatile 通过对 volatile 变量的读写建立 happens-before 关系，一个线程对 volatile 变量的写操作，对后续其他线程对该变量的读操作立即可见。同时 volatile 写前的普通操作不会被重排序到 volatile 写之后，volatile 读后的操作不会被重排序到 volatile 读之前，从而保证一定的有序性。但是 volatile 不保证复合操作的原子性。


- Java内存模型规定8个规则
  1. 程序顺序规则
  2. 锁规则（最重要）
  3. volatile规则（高频）
  4. 线程启动规则
  5. 线程终止规则
  6. 中断规则
  7. 对象初始化规则
  8. 传递性
    

    
## as-if-serial(顺序一致性原则/单线程语义)
 JMM随便优化，但需要保证单线程执行结果不变。
 


