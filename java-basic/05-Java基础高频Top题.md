# 05-Java基础高频Top题

## 1. HashMap 和 ConcurrentHashMap

### HashMap 底层结构

答法：

> JDK 8 的 HashMap 底层是数组、链表、红黑树。通过 hash 定位数组下标，冲突时使用链表，链表长度达到 8 且数组容量达到 64 时转红黑树。

### ConcurrentHashMap 如何保证线程安全

答法：

> JDK 8 通过 CAS + synchronized 保证线程安全。桶为空时 CAS 插入，桶不为空时锁住桶头节点。get 通常不加锁，依赖 volatile 保证可见性。

### HashMap 为什么线程不安全

答法：

> 多线程 put 可能导致数据覆盖、丢失、size 不准和扩容异常。JDK 8 虽然避免了 JDK 7 头插法成环问题，但仍然不是线程安全的。

## 2. equals 和 hashCode

### 两者关系

答法：

> equals 相等的对象，hashCode 必须相等；hashCode 相等的对象，equals 不一定相等。HashMap 先通过 hashCode 定位桶，再通过 equals 判断 key 是否相等。

### 只重写 equals 不重写 hashCode 会怎样

答法：

> 可能导致 HashMap、HashSet 中逻辑相等的对象落到不同桶里，出现 get 不到、去重失败等问题。

## 3. String、StringBuilder、StringBuffer

### 区别

`String`：

- 不可变。
- 字符串拼接会产生新对象。

`StringBuilder`：

- 可变。
- 线程不安全。
- 性能较好。

`StringBuffer`：

- 可变。
- 方法加 synchronized。
- 线程安全但性能较低。

答法：

> 单线程大量拼接用 StringBuilder，多线程共享拼接才考虑 StringBuffer。普通少量字符串拼接，编译器会做优化，不必过度设计。

## 4. final 关键字

可以修饰：

- 类：不能被继承。
- 方法：不能被重写。
- 变量：引用不能改变。

注意：

```java
final List<String> list = new ArrayList<>();
list.add("a");
```

`final` 修饰引用类型时，引用地址不能变，但对象内部状态可以变。

## 5. 抽象类和接口

抽象类：

- 可以有成员变量。
- 可以有构造方法。
- 适合表达“是什么”。
- 单继承。

接口：

- 表达能力约束。
- Java 8 后可以有 default 方法。
- 支持多实现。

答法：

> 抽象类适合抽取一类对象的公共状态和行为，接口适合定义能力和规范。比如模板流程可以用抽象类，插件扩展点更适合接口。

## 6. 重载和重写

重载：

- 同一个类中。
- 方法名相同。
- 参数列表不同。
- 编译期确定。

重写：

- 子类重写父类方法。
- 方法签名相同。
- 运行期动态绑定。

## 7. == 和 equals

`==`：

- 基本类型比较值。
- 引用类型比较地址。

`equals`：

- 默认也是比较地址。
- 很多类重写后比较内容，比如 String。

## 8. Java 异常体系

顶层：

```text
Throwable
  Error
  Exception
    RuntimeException
    Checked Exception
```

`Error`：

- JVM 级严重错误。
- 一般不捕获。

`RuntimeException`：

- 运行时异常。
- 如 NullPointerException、IllegalArgumentException。

Checked Exception：

- 编译期要求处理。
- 如 IOException、SQLException。

面试表达：

> 业务代码一般捕获可恢复异常，不建议捕获 Throwable 或 Error。事务场景下还要注意 Spring 默认只对 RuntimeException 和 Error 回滚。

## 9. 深拷贝和浅拷贝

浅拷贝：

- 复制对象本身。
- 引用字段仍指向同一对象。

深拷贝：

- 对象和内部引用对象都复制。

常见实现：

- 手动复制。
- 构造方法。
- 序列化。
- JSON 转换。

注意：

> JSON 深拷贝简单但性能和类型精度要注意，不适合所有场景。

## 10. 反射

反射可以在运行时获取类信息、创建对象、调用方法、访问字段。

常见用途：

- Spring IOC。
- MyBatis 映射。
- 注解处理。
- JSON 序列化。

缺点：

- 性能低于直接调用。
- 破坏封装。
- 编译期不容易发现问题。

## 11. 泛型

Java 泛型是类型擦除。

编译期做类型检查，运行期泛型信息大多被擦除。

例如：

```java
List<String> list1;
List<Integer> list2;
```

运行期主要都是 `List`。

常见问题：

> 为什么不能 `new T()`？因为类型擦除后运行期不知道 T 的具体类型。

## 12. volatile

作用：

- 保证可见性。
- 禁止指令重排序。
- 不保证复合操作原子性。

不能保证：

```java
count++;
```

因为 `count++` 包含读、加、写多个步骤。

适合：

- 状态标记。
- 单例双重检查中的 instance 可见性和禁止重排。

## 13. synchronized

作用：

- 保证原子性。
- 保证可见性。
- 保证有序性。

锁对象：

- 普通方法：锁当前实例。
- 静态方法：锁 Class 对象。
- 代码块：锁指定对象。

## 14. ThreadLocal

作用：

> 为每个线程保存独立变量副本。

常见用途：

- 用户上下文。
- traceId。
- 数据源路由标记。
- 事务上下文。

风险：

- 在线程池中使用后不 remove，可能内存泄漏或数据串用。

面试表达：

> ThreadLocal 适合线程内上下文传递，不适合跨线程传递。在线程池中必须在 finally 里 remove。

## 15. 线程池

重点必须会：

- 七个参数。
- 提交流程。
- 队列选择。
- 拒绝策略。
- 为什么不用 Executors。
- 项目参数怎么估。

一句话：

> 线程池不是为了让任务无限堆积，而是为了控制并发和保护系统。

## 16. 面试优先级

优先级 P0：

- HashMap。
- ConcurrentHashMap。
- equals/hashCode。
- ArrayList/LinkedList。
- 线程池。
- volatile/synchronized。
- ThreadLocal。

优先级 P1：

- String/StringBuilder/StringBuffer。
- 接口和抽象类。
- 异常体系。
- 泛型擦除。
- 反射。
- 深浅拷贝。

优先级 P2：

- 类加载细节。
- SPI。
- 动态代理。
- Java 8 Stream。
- CompletableFuture。

