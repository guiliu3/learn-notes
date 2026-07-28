package com.learn.thread;

/**
 *  演示 volatile是否具有原子性
 */
public class TestVolatileIncDemo {

    private static volatile int number =0;

    private static  void inc(){
        number++;
    }

    public static void main(String[] args) {

        for(int i=0;i<10;i++){

            new Thread(()->{

                for(int j=0;j<1000;j++){
                    inc();
                }
            },"t"+String.valueOf(i)).start();
        }

        while(Thread.activeCount()>2){
            Thread.yield();
        }

        System.out.println(Thread.currentThread().getName() +
                " final number result = " + number);
    }

}
