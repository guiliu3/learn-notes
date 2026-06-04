# 四、JUC 并发                                            

## 一、Java 内存模型（JMM）
### 1. 为什么需要 JMM（CPU 缓存、指令重排问题）
现代CPU为了 提升性能，做了2件事情，直接导致并发问题：
- 多级缓存
- 指令重排序

多级缓存
 ```txet
  CPU Core1         CPU Core2
    L1 Cache           L1 Cache
    L2 Cache           L2 Cache
           L3 Cache(共享)
             主内存（RAM）
  
  每个核心有自己的缓存，变量从主内存读到缓存后，各自修改，互相看不到。这就是可见性问题。
  ``` 
>**指令重排序**:CPU和编译器为了优化执行效率，会对指令重新排序，只保证单线程结果正确，但多线程会出现问题，这就是有序性问题。

总结：JMM 就是Java对这2个问题定义的一套规范，告诉JVM和CPU，什么时必须把数据同步到主内存，什么时候不能重排序。

### 2. 主内存与工作内存
JMM 将内存抽象成两层：
- 主内存：存放所有共享变量（实例字段、静态字段）
- 工作内存：每一个线程私有，存放该线程用到的变量副本

线程读变量：从主内存拷贝到工作内存
线程写变量：写到工作内存，再刷回主内存

注意：工作内存是JMM的抽象概念。

### 3. 三大特性：可见性、原子性、有序性
- 可见性：一个线程修改了共享变量，其他线程能立即看到。
- 原子性：一个操作不可被中断，要么全部执行，要么不执行。
- 有序性：程序执行顺序按代码顺序，不能随意重排。

### 4. happens-before 原则（8条规则）
JMM用happends-before来描述两个操作之间的内存可见性：如果A happens-before B,则A的结果对B可见。

8条规则

| 规则          | 说明                                              |
|-------------|-------------------------------------------------|
| 程序顺序规则      | 同一个线程内，前面的操作happens-before 后面的操作                |
| Monitor 锁规则 | unlock happends-before 后续的lock(synchronized的基础) |
| volatile规则  | 写volatile变量happens-before后续读该变量                 |
| 传递性规则       | A hb B ,B hb C, 则 A hb C                        |
| 线程启动规则      | Thread.start() happens-before 线程内任何操作           |
| 线程终止规则      | 线程所有操作 happens-before Thread.join()返回           |
| 中断规则        | interrupt() happens-before 检测到中断                |
| 对象终结规则      | 构造方法结束 happens-before finalize()                |

```text
天然成立（不用管）
├── 规则1：同线程顺序
├── 规则5：线程启动
├── 规则6：线程终止
├── 规则7：中断
└── 规则8：对象终结

需要代码触发
├── 规则2：synchronized（加锁/解锁）
└── 规则3：volatile（读/写）

组合放大
└── 规则4：传递性（把前面的规则串联起来）
```

> 总结: 8 条规则是 JVM 的承诺，前 5 条是天然成立的，不需要你写额外代码。真正需要你主动触发的只有两条：用 synchronized 触发锁规则，用 volatile 触发 volatile 规则。其余的靠传递性自动推导。多线程出问题，本质上都是没有触发任何规则，JMM
不做保证。


**Q:JMM 与 JVM 内存结构是一回事吗？**
>答：不是。JVM内存结构讲的是堆、栈、方法区等物理划分。JMM是一套抽象规范，描述线程之间如何共享数据、什么时候可见，解决的是并发可见性、有序性问题。

**Q:happens-before 是说操作的执行顺序吗?**
>答：不完全是。happens-before是内存可见性的保证，不一定代表物理执行顺序，A happens-before B，只保证A的结果对B可见，CPU实际执行时仍然肯呢个重排，但结果必须等价于按顺序执行。
 

## 二、volatile
### 1. 作用：可见性 + 禁止指令重排
 可见性：
- 写：把工作内存中的值理科刷新到主存
- 读：每次都从主内存重新读，不用缓存。

>参考代码：src/main/java/com/learn/jvm/volatile_demo/VolatileVisibilityDemo.java


### 2. 底层原理：内存屏障（LoadLoad、StoreStore、LoadStore、StoreLoad）
如图所示：
![内存屏障](../img/jvm/volatile-memory-barrier.png)
比喻：
![内存屏障的比喻](../img/jvm/volatile-barrier-analogy.png)

>总结：volatile 底层靠的是内存屏障。写 volatile 前插 StoreStore 保证之前的写不乱序，后插 StoreLoad 防止与后续读重排。读 volatile 前插 LoadLoad 保证之前的读不乱序，后插 LoadStore 防止与后续写重排.

### 3. 为什么不保证原子性（i++ 反例）

```
volatile int i = 0;

// 10个线程各执行1000次 i++
// 最终结果不是 10000
```
i++ 分三步，volatile 只保证每次读都从主内存读、每次写都刷主内存，但三步之间没有锁：

例如：
- 线程A：读 i=5
- 线程B：读 i=5（A还没写回）
- 线程A：计算 5+1=6，写回 i=6
- 线程B：计算 5+1=6，写回 i=6  ← 丢失了一次自增
- 两个线程都读到了 5，各自加一写回 6，本来应该是 7，实际是 6，丢了一次。

>参考代码：src/main/java/com/learn/jvm/volatile_demo/VolatileAtomicDemo.java


### 4. 使用场景：状态标志位、DCL 双重检查锁
- DCL 双重检查锁
```java
  public class Singleton {
      private static Singleton instance;  // 没有 volatile

      public static Singleton getInstance() {
          if (instance == null) {                    // 第一次检查
              synchronized (Singleton.class) {
                  if (instance == null) {            // 第二次检查
                      instance = new Singleton();    // 问题在这里
                  }
              }
          }
          return instance;
      }
  }
```
instance = new Singleton() 看起来是一行，实际上是三步：

1. 分配内存空间
2. 初始化对象（执行构造方法）
3. 将引用指向内存地址

JVM 可能把步骤 2 和 3 重排序，变成 1 → 3 → 2：

1. 线程A：1.分配内存  3.引用指向地址（对象还没初始化完！）
2. 线程B：第一次检查 instance != null → 直接返回
3. 线程B：拿到的是未初始化完的对象 → NPE 或数据异常

加上 volatile 之后：

private static volatile Singleton instance;  // 加 volatile

volatile 的 StoreStore 屏障禁止了 2 和 3 的重排，保证对象初始化完成后才把引用指向它，线程 B 要么看到 null，要么看到完整对象，不会看到中间状态。


**Q：volatile 为什么不能保证原子性？**
> 答：volatile 只保证单次读/写的可见性，但 i++ 这类复合操作（读-改-写三步）之间没有互斥保护，多线程并发执行时步骤会交叉，导致结果丢失。需要原子性要用 synchronized 或 AtomicInteger。

**Q：DCL 为什么需要 volatile？**
> 答：new 对象分三步：分配内存、初始化对象、引用赋值。JVM 可能将后两步重排，导致另一个线程拿到引用时对象还未初始化完。volatile 的 StoreStore 屏障禁止这个重排，保证对象完全初始化后引用才对外可见。

**Q：volatile 适合什么场景？**
>答：两类场景。一是状态标志位（只有写，不依赖当前值），二是 DCL 单例（配合 synchronized 使用）。涉及自增、累加等依赖当前值的操作，不能用 volatile，要用原子类或锁


## 三、synchronized
1. 使用方式：修饰方法 vs 修饰代码块
2. 底层原理：对象头 Mark Word、Monitor
3. 锁升级过程：无锁 → 偏向锁 → 轻量级锁 → 重量级锁
4. synchronized vs volatile 对比

## 四、CAS 与原子类
1. CAS 原理：比较并交换（Unsafe 类）
2. ABA 问题与 AtomicStampedReference 解决方案
3. 原子类分类：基本类型、引用类型、数组类型、字段更新器
4. CAS 的缺点：自旋开销、只能保证单变量原子性

## 五、AQS（AbstractQueuedSynchronizer）
1. AQS 核心结构：state + CLH 队列
2. 独占模式 vs 共享模式
3. ReentrantLock 原理：公平锁 vs 非公平锁
4. ReentrantReadWriteLock：读写分离
5. synchronized vs ReentrantLock 对比

## 六、并发工具类
1. CountDownLatch：一次性倒计时，不可重置
2. CyclicBarrier：可重置屏障，所有线程到齐才放行
3. Semaphore：限流，控制并发数量
4. CountDownLatch vs CyclicBarrier 对比

## 七、线程池
1. 为什么用线程池（资源复用、控制并发数）
2. ThreadPoolExecutor 七大核心参数
3. 四种拒绝策略
4. 线程池执行流程（核心线程 → 队列 → 最大线程 → 拒绝）
5. 四种内置线程池（FixedThreadPool、CachedThreadPool、ScheduledThreadPool、SingleThreadExecutor）及各自的坑
6. 线程池参数如何合理设置（CPU密集型 vs IO密集型）

## 八、常见面试题
Q：synchronized 锁升级的过程？
Q：volatile 为什么不能保证原子性？
Q：DCL 单例为什么要加 volatile？
Q：ReentrantLock 和 synchronized 的区别？
Q：线程池的执行流程？
Q：为什么不建议用 Executors 创建线程池？
Q：AQS 的核心原理？
Q：CAS 的 ABA 问题怎么解决？

## 九. 学习重点
| 优先级  | 知识点                                       |
|------|-------------------------------------------|
| 必须掌握 | volatile、synchronized 锁升级、线程池七大参数、执行流程    |
| 重点掌握 | JMM、CAS/ABA、ReentrantLock vs synchronized |
| 了解即可 | AQS 源码细节、并发工具类内部原理                        |