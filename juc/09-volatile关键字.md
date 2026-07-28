# volatile 关键字

  volatile是一个变量修饰符，无法修饰方法和代码块，可以保证有序性、可见性，无法保证原子性，因为它不是锁，没有做任何的原子性处理。
 
## 无法保证原子性
参考源码：com.learn.thread.TestVolatileIncDemo
修复方法：通过在 com.learn.thread.TestVolatileIncDemo.inc 增加 synchronized关键字。

## 如何保证原子性自增：
- AtomicLong
- AtomicInteger

## volatile 如何保证可见性和有序性的
- 可见性：使用volatile修饰的变量进行写操作时，JVM都会将工作内存的值强制刷新到主存。
- 有序性： volatile是通过内存屏障去禁止指令重排序，严格按照代码执行。

## 有了synchronized为什么还需要volatile
 经典的单例模式就可以引出这个问题
- 双重检查锁（Double Check Lock） DCL
  
```text
 DCL代码:
 
    public class Singleton {  
      private static Singleton singleton;  
       private Singleton (){}  
       public static Singleton getSingleton() {  
       if (singleton == null) {  
           synchronized (Singleton.class) {  
               if (singleton == null) {  
                   singleton = new Singleton();  
               }  
           }  
       }  
       return singleton;  
       }  
   }
```
- 问题原因：synchronized保证是多个线程创建对象的互斥性，但是无法保证创建对象时，产生的指令重排序，导致多线程下，产生拿到没有初始化的对象。导致NP问题。
- 问题根本：new Singleton();  并不是一个原子性操作，分为3步：分配内存、初始化对象、赋值引用变量。会产生指令重排序问题。 




