# 02-ConcurrentHashMap核心原理

## 1. 面试先给结论

`ConcurrentHashMap` 是线程安全的 Map 实现。JDK 7 使用 Segment 分段锁，JDK 8 取消 Segment，改为数组 + 链表 + 红黑树，并使用 CAS + synchronized 控制并发。

面试表达可以这样说：

> ConcurrentHashMap 是高并发场景下常用的线程安全 Map。JDK 7 主要靠 Segment 分段锁降低锁粒度，JDK 8 改成 Node 数组结构，put 时如果桶为空用 CAS 插入，如果桶不为空就锁住桶的头节点进行插入或更新。这样锁粒度从 Segment 降到桶级别，并且读操作大多不加锁。

## 2. JDK 7 和 JDK 8 区别

### 2.1 JDK 7

结构：

```text
ConcurrentHashMap
  Segment[]
    HashEntry[]
      链表
```

特点：

- Segment 继承 ReentrantLock。
- 每个 Segment 管理一部分桶。
- 并发度由 Segment 数量决定。
- 锁粒度是 Segment。

### 2.2 JDK 8

结构：

```text
ConcurrentHashMap
  Node[]
    链表 / 红黑树
```

特点：

- 取消 Segment。
- 使用 CAS + synchronized。
- 锁粒度是桶。
- 支持红黑树。
- 扩容支持多线程协助迁移。

## 3. put 流程

简化流程：

1. 判断 table 是否初始化，没有则初始化。
2. 根据 key 计算 hash。
3. 根据 hash 定位桶下标。
4. 如果桶为空，用 CAS 插入。
5. 如果桶不为空：
   - 如果正在扩容，帮助扩容。
   - 否则锁住桶头节点。
   - 在链表或红黑树中查找 key。
   - key 相同则覆盖。
   - key 不同则新增。
6. 插入后更新计数。
7. 判断是否需要扩容。

面试表达：

> JDK 8 的 ConcurrentHashMap put 时，桶为空就用 CAS 插入，不加锁；桶不为空就 synchronized 锁住桶的头节点，锁粒度比较小。扩容时如果发现当前桶正在迁移，线程还会协助扩容。

## 4. get 是否加锁

`get` 基本不加锁。

流程：

1. 根据 key 计算 hash。
2. 定位桶。
3. 如果第一个节点匹配，直接返回。
4. 如果是链表，遍历链表。
5. 如果是红黑树，按树查找。

为什么不加锁也能读：

- Node 的 key 和 hash 是 final。
- value 和 next 使用 volatile 保证可见性。
- 插入和更新通过 CAS 或 synchronized 保证安全发布。

面试表达：

> ConcurrentHashMap 的 get 通常不加锁，依赖 volatile 保证可见性。它可以在并发读写下保证读到的是某个时刻的安全结果，但不保证全局强一致快照。

## 5. size 为什么复杂

并发环境下统计 size 很难，因为多个线程可能同时 put/remove。

JDK 8 使用类似 LongAdder 的思想：

- baseCount。
- CounterCell 数组。

低并发时更新 baseCount。

竞争激烈时分散到多个 CounterCell，减少 CAS 冲突。

size 统计时汇总 baseCount 和 CounterCell。

注意：

> 并发修改下 size 只是近似值，不适合用来做强一致业务判断。

## 6. 扩容机制

ConcurrentHashMap 扩容比较复杂，核心是多线程协助迁移。

扩容触发：

- 元素数量超过阈值。

迁移过程：

- 创建新数组。
- 多个线程分段迁移旧桶。
- 迁移完成的桶会放置 ForwardingNode。
- 其他线程访问到 ForwardingNode 时，会帮助迁移或去新表查找。

面试表达：

> ConcurrentHashMap 扩容时不是单线程完成迁移，而是允许多个线程协助扩容。迁移过的桶会放 ForwardingNode，其他线程发现后可以帮助迁移，这样降低单线程扩容带来的阻塞。

## 7. 为什么用 synchronized 而不是 ReentrantLock

JDK 8 中 synchronized 已经做了大量优化：

- 偏向锁。
- 轻量级锁。
- 锁消除。
- 锁粗化。

ConcurrentHashMap 锁的粒度是桶头节点，锁范围很小。

使用 synchronized：

- 代码更简单。
- JVM 可优化。
- 不需要额外 Lock 对象。

## 8. ConcurrentHashMap 能存 null 吗

不能存 null key，也不能存 null value。

原因：

> 在并发场景下，get 返回 null 无法区分是 key 不存在，还是 value 本身为 null。

HashMap 可以存一个 null key 和多个 null value。

ConcurrentHashMap 不允许 null，是为了避免并发语义歧义。

## 9. 常见使用误区

### 9.1 复合操作不是天然线程安全

错误：

```java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

这两个操作组合起来不是原子的。

正确：

```java
map.putIfAbsent(key, value);
```

或者：

```java
map.computeIfAbsent(key, k -> value);
```

### 9.2 size 不适合做强一致判断

错误：

```java
if (map.size() == 0) {
    // 认为一定没有任务
}
```

并发修改下 size 可能只是瞬时结果。

## 10. 高频问题

### 10.1 ConcurrentHashMap 如何保证线程安全

答法：

> JDK 8 主要通过 CAS + synchronized 保证线程安全。桶为空时用 CAS 插入，桶不为空时锁住桶头节点进行链表或红黑树更新。value 和 next 使用 volatile 保证可见性。

### 10.2 JDK 7 和 JDK 8 有什么区别

答法：

> JDK 7 使用 Segment 分段锁，锁粒度是 Segment；JDK 8 取消 Segment，改为数组、链表、红黑树，使用 CAS 和 synchronized，锁粒度降到桶级别，并支持多线程协助扩容。

### 10.3 get 要不要加锁

答法：

> get 基本不加锁，依赖 volatile 保证可见性。它能保证并发环境下读到安全结果，但不是全局强一致快照。

### 10.4 为什么不能存 null

答法：

> 因为并发场景下 get 返回 null 无法区分 key 不存在还是 value 本身为 null，所以 ConcurrentHashMap 禁止 null key 和 null value。

### 10.5 putIfAbsent 和 containsKey + put 有什么区别

答法：

> putIfAbsent 是原子操作，containsKey + put 是两个独立操作，并发下可能多个线程同时判断不存在然后覆盖写入。

## 11. 你要背下来的 1 分钟版本

> ConcurrentHashMap 是线程安全的 Map。JDK 7 用 Segment 分段锁，JDK 8 改成数组、链表、红黑树，并通过 CAS + synchronized 控制并发。put 时桶为空用 CAS 插入，桶不为空锁住桶头节点，所以锁粒度比较小。get 通常不加锁，依赖 volatile 保证可见性。ConcurrentHashMap 不允许 null key 和 null value，因为并发下 get 返回 null 无法区分不存在还是值就是 null。

