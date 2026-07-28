package com.learn.thread;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 适用于 JDK 8 的 ReentrantLock 特性验证示例。
 *
 * 验证内容：
 * 1. 公平锁：等待时间较早的线程优先获取锁；
 * 2. 非公平锁：新来的线程允许与队列中的线程竞争；
 * 3. 可重入：同一个线程可以多次获取同一把锁；
 * 4. 查看 JDK 8 AQS 同步队列中的线程信息。
 */
public class TestReentrantLockJdk8 {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 1. 可重入验证 ==========");
        testReentrant();

//        Thread.sleep(1000L);
//        System.out.println("\n========== 2. 公平锁验证 ==========");
//        testFairLock();
//
//        Thread.sleep(1000L);
//        System.out.println("\n========== 3. 非公平锁验证 ==========");
//        testUnfairLock();
    }

    /**
     * 验证锁重入。
     * state 会随着同一线程重复 lock() 从 1 增加到 2，
     * 每执行一次 unlock() 再减 1。
     */
    private static void testReentrant() {
        MyReentrantLock lock = new MyReentrantLock(false);

        lock.lock();
        try {
            print("第一次获得锁，holdCount=" + lock.getHoldCount()
                    + "，state=" + lock.getSyncState());

            reentrantMethod(lock);

            print("内层方法结束，holdCount=" + lock.getHoldCount()
                    + "，state=" + lock.getSyncState());
        } finally {
            lock.unlock();
            print("外层释放锁，holdCount=" + lock.getHoldCount()
                    + "，state=" + lock.getSyncState());
        }
    }

    private static void reentrantMethod(MyReentrantLock lock) {
        lock.lock();
        try {
            print("第二次获得同一把锁，holdCount=" + lock.getHoldCount()
                    + "，state=" + lock.getSyncState());
        } finally {
            lock.unlock();
            print("内层释放一次锁，holdCount=" + lock.getHoldCount()
                    + "，state=" + lock.getSyncState());
        }
    }

    /**
     * 公平锁验证。
     * 主线程先占有锁，让 t1、t2、t3 按顺序进入同步队列，
     * 主线程释放后，通常按照 t1 -> t2 -> t3 的顺序获取锁。
     */
    private static void testFairLock() throws InterruptedException {
        MyReentrantLock lock = new MyReentrantLock(true);
        runQueueTest(lock);
    }

    /**
     * 非公平锁验证。
     * 非公平并不代表一定乱序，而是新来的线程可以插队竞争。
     * 因此需要多运行几次，才更容易观察到与排队顺序不一致的情况。
     */
    private static void testUnfairLock() throws InterruptedException {
        MyReentrantLock lock = new MyReentrantLock(false);
        runQueueTest(lock);
    }

    private static void runQueueTest(final MyReentrantLock lock)
            throws InterruptedException {

        final CountDownLatch finished = new CountDownLatch(3);

        lock.lock();
        try {
            print("主线程先获得锁，fair=" + lock.isFair());

            startWorker(lock, "t1", finished);
            Thread.sleep(100L);

            startWorker(lock, "t2", finished);
            Thread.sleep(100L);

            startWorker(lock, "t3", finished);
            Thread.sleep(300L);

            print("释放前的同步队列：" + lock.getQueuedInfo());
        } finally {
            print("主线程释放锁");
            lock.unlock();
        }

        finished.await();
        print("本轮执行结束，fair=" + lock.isFair());
    }

    private static void startWorker(final MyReentrantLock lock,
                                    String threadName,
                                    final CountDownLatch finished) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                print("准备获取锁");
                lock.lock();
                try {
                    print("成功获取锁，队列剩余：" + lock.getQueuedInfo());
                    sleep(200L);
                } finally {
                    print("释放锁");
                    lock.unlock();
                    finished.countDown();
                }
            }
        }, threadName);
        thread.start();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void print(String message) {
        Thread thread = Thread.currentThread();
        System.out.println("[" + thread.getName() + "]["
                + thread.getState() + "] " + message);
    }

    /**
     * 为了学习源码，增加查看 AQS state 和同步队列的方法。
     *
     * 注意：下面使用的是 JDK 8 的字段名称：
     * Node.thread
     * Node.waitStatus
     * Node.next
     * Node.nextWaiter
     */
    private static class MyReentrantLock extends ReentrantLock {

        private static final long serialVersionUID = 1L;

        private MyReentrantLock(boolean fair) {
            super(fair);
        }

        private int getSyncState() {
            try {
                AbstractQueuedSynchronizer sync = getSync();
                Field stateField =
                        AbstractQueuedSynchronizer.class.getDeclaredField("state");
                stateField.setAccessible(true);
                return stateField.getInt(sync);
            } catch (Exception e) {
                throw new RuntimeException("读取 AQS state 失败", e);
            }
        }

        private String getQueuedInfo() {
            List<String> nodes = new ArrayList<String>();

            try {
                AbstractQueuedSynchronizer sync = getSync();
                Field headField =
                        AbstractQueuedSynchronizer.class.getDeclaredField("head");
                headField.setAccessible(true);

                Class<?> nodeClass = Class.forName(
                        "java.util.concurrent.locks.AbstractQueuedSynchronizer$Node");

                // JDK 8 中字段名叫 thread、waitStatus、next。
                Field threadField = nodeClass.getDeclaredField("thread");
                Field waitStatusField = nodeClass.getDeclaredField("waitStatus");
                Field nextField = nodeClass.getDeclaredField("next");

                threadField.setAccessible(true);
                waitStatusField.setAccessible(true);
                nextField.setAccessible(true);

                Object node = headField.get(sync);
                while (node != null) {
                    Thread thread = (Thread) threadField.get(node);
                    int waitStatus = waitStatusField.getInt(node);

                    String threadName = thread == null
                            ? "head"
                            : thread.getName();

                    nodes.add("(" + threadName
                            + ", waitStatus=" + waitStatus + ")");

                    node = nextField.get(node);
                }
            } catch (Exception e) {
                throw new RuntimeException("读取 AQS 同步队列失败", e);
            }

            return nodes.isEmpty() ? "[]" : nodes.toString();
        }

        /**
         * JDK 8 中 ReentrantLock 内部持有 Sync 类型的 sync 字段，
         * Sync 又继承了 AbstractQueuedSynchronizer。
         */
        private AbstractQueuedSynchronizer getSync() throws Exception {
            Field syncField = ReentrantLock.class.getDeclaredField("sync");
            syncField.setAccessible(true);
            return (AbstractQueuedSynchronizer) syncField.get(this);
        }

        /**
         * 保留命名 Condition 的写法，方便后续继续观察条件队列。
         */
        @SuppressWarnings("unused")
        private Condition newCondition(String name) {
            return super.newCondition();
        }
    }
}
