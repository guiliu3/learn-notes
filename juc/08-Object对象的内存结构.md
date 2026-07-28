# Object对象的内存结构

  Object Header（对象头）可以理解为对象的身份证，然后Mark Word是对象头的很重一部分。

## Java对象的内存结构
```text
+------------------------+
|      Object Header     |   对象头
|------------------------|
| Mark Word              |   ★ 存放锁信息
| Klass Pointer          |   指向Class对象
| (数组还有Length)        |
+------------------------+
|      Instance Data     |   成员变量
+------------------------+
|      Padding           |   内存对齐
+------------------------+

```
说明：synchronized 并不是给对象新增一个Lock，而是修改对象头的Mark word

## Mark Word（标记字段）
  它是Object Header的一部分，里面存有GC标记、线程ID、锁状态等信息。一般是64bit

- 四种锁的状态的标志位判断,低2位判断锁状态，但是01可以表示无锁和偏向锁，所需要额外加一位区分。
  1. 无锁： lock：01 biased:0 
  2. 偏向： lock:01 biased:1
  3. 轻量级： lock:00 
  4. 重量级： lock:10

