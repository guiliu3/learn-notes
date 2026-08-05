## ThreadLocal的底层结构（回忆）

```text
Thread
 |
 |-- ThreadLocalMap
          |
          Entry[]



static class Entry extends WeakReference<ThreadLocal<?>> {

    Object value;

}

```

Entry extends WeakReference<ThreadLocal>

- 整体结构如下：
```text
Thread
 |
 ThreadLocalMap
 |
 Entry

      key
       |
       |(弱引用)
       |
    ThreadLocal对象


      value
       |
       |(强引用)
       |
    User对象

```

如果 ThreadLocal 对象被回收了，Value 仍然被 ThreadLocalMap 持有，导致 Value 无法释放。在线程长期存活（比如线程池）时，就可能造成内存泄漏。

- 解决办法：需要在finally中调用remove进行主动清理。


