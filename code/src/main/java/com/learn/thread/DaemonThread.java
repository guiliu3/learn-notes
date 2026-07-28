package com.learn.thread;

/**
 * 演示若JVM只有守护线程时，JVM会退出
 *
 *
 *
 */
public class DaemonThread {

    public static void main(String[] args) throws InterruptedException {

       // noDaemonThread();
        DaemonThread();
    }

    public static void noDaemonThread(){
        Thread childThread = new Thread(new Runnable(){

            @Override
            public void run() {
                while(true){

                    System.out.println("I am Children Thread");
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        },"t1");

        childThread.start();
        System.out.println(" main thread is Over");


    }

    public static void DaemonThread() throws InterruptedException {
        Thread childThread = new Thread(new Runnable(){

            @Override
            public void run() {
                while(true){

                    System.out.println("I am Children Thread");
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        },"t1");
        childThread.setDaemon(true);
        childThread.start();

        Thread.sleep(2000L);

        System.out.println(" main thread is Over");

    }
}
