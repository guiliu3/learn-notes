package com.learn.thread;

public class SynchronizedBytecodeDemo {

    private final Object lock = new Object();

    /**
     * 实例同步方法。
     * 锁对象是当前实例 this。
     */
    public synchronized void syncMethod() {
        System.out.println("syncMethod");
    }

    /**
     * 静态同步方法。
     * 锁对象是 SynchronizedBytecodeDemo.class。
     */
    public static synchronized void staticSyncMethod() {
        System.out.println("staticSyncMethod");
    }

    /**
     * 同步代码块。
     * 锁对象是 lock。
     */
    public void syncBlock() {
        synchronized (lock) {
            System.out.println("syncBlock");
        }
    }

    /**
     * 同步代码块中主动抛出异常。
     */
    public void syncBlockWithException() {
        synchronized (lock) {
            System.out.println("准备抛出异常");
            throw new RuntimeException("测试异常");
        }
    }

    public static void main(String[] args) {
        SynchronizedBytecodeDemo demo =
                new SynchronizedBytecodeDemo();

        demo.syncMethod();
        staticSyncMethod();
        demo.syncBlock();

        try {
            demo.syncBlockWithException();
        } catch (RuntimeException e) {
            System.out.println("捕获异常：" + e.getMessage());
        }
    }
}