package com.learn.thread;

/**
 * 演示notifyAll和notify的区别
 */
public class NotifyAndNotifyAllExample {


    private static final Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {

        // 创建 3 个等待线程
        for (int i = 1; i <= 3; i++) {
            String threadName = "等待线程-" + i;

            new Thread(() -> {
                synchronized (LOCK) {
                    try {
                        System.out.println(Thread.currentThread().getName()
                                + " 获取锁，调用 wait()，进入等待状态");

                        LOCK.wait();

                        System.out.println(Thread.currentThread().getName()
                                + " 被唤醒，并重新获取锁，继续执行");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, threadName).start();
        }

        // 确保 3 个线程都已经进入 wait()
        Thread.sleep(1000);

        testNotify();

        // 改成下面这行，测试 notifyAll()
        // testNotifyAll();
    }

    /**
     * notify：随机唤醒一个在该锁对象上等待的线程
     */
    private static void testNotify() {
        synchronized (LOCK) {
            System.out.println("\n主线程调用 notify()");

            LOCK.notify();

            System.out.println("notify() 调用完成，但主线程还没有释放锁");
        }

        System.out.println("主线程退出 synchronized，释放锁");
    }

    /**
     * notifyAll：唤醒所有在该锁对象上等待的线程
     */
    private static void testNotifyAll() {
        synchronized (LOCK) {
            System.out.println("\n主线程调用 notifyAll()");

            LOCK.notifyAll();

            System.out.println("notifyAll() 调用完成，但主线程还没有释放锁");
        }

        System.out.println("主线程退出 synchronized，释放锁");
    }


}
