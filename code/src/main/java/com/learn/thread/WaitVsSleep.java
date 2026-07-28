package com.learn.thread;

import com.learn.utils.ThreadLogUtils;

/**
 * 演示 Wait 和 Sleep的区别。
 * 1.wait必须配合synchronized使用，否则报错
 * 2.wait方法使用后会释放掉锁，而sleep不会。
 * 3.wait和sleep都会被Interrupted打断唤醒
 */

public class WaitVsSleep {

    // 对象锁
    static final  Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {
//        illegalWait();
//        waiting();
        testSleep();
    }


    /**
     * 输出 IllegalMonitorStateException
     * @throws InterruptedException
     */
    private  static void illegalWait() throws InterruptedException {
        LOCK.wait();
    }

    private static void waiting() throws InterruptedException {
        Thread t1 = new Thread(()->{
            synchronized (LOCK){
                ThreadLogUtils.print("开始执行t1的wait方法");
                try {
                    LOCK.wait(5000L);
                    ThreadLogUtils.print("子线程等待时间已过，可以尝试获取锁");
                } catch (InterruptedException e) {
                    ThreadLogUtils.print("t1线程被InterruptedException打断");
                    e.printStackTrace();
                }
            }
        },"t1");

        t1.start();
        // 主线程延迟100，为了让子线程先拿到LOCK
        Thread.sleep(100);
        synchronized (LOCK){
            ThreadLogUtils.print("主线程获取到了锁");
        }

    }

    private static void testSleep() throws InterruptedException {
        Thread t1 = new Thread(()->{
            synchronized (LOCK){
                ThreadLogUtils.print("开始执行t1的sleep方法");
                try {
                    Thread.sleep(5000);
                    ThreadLogUtils.print("子线程等待时间已过，继续执行");
                } catch (InterruptedException e) {
                    ThreadLogUtils.print("t1线程被InterruptedException打断");
                    e.printStackTrace();
                }
            }
        },"t1");

        t1.start();
        // 主线程延迟100，为了让子线程先拿到LOCK
        Thread.sleep(100);
        synchronized (LOCK){
            ThreadLogUtils.print("主线程获取到了锁");
        }
        System.out.println("主线程运行结束");
    }

}
