package com.learn.thread;

/**
 *   演示下 ThreadLocal 父子线程通信
 */
public class ShareDateParentThreadDemo {

  public static final ThreadLocal<Integer> shareData = new ThreadLocal<>();


    public static void main(String[] args) {

        shareData.set(0);
       Thread child =  new Thread(()->{
           System.out.println("shareDate in child Thread"+ shareData.get());
            shareData.set(shareData.get()+1);
           System.out.println("shareDate in child after incremnet"+shareData.get());

        },"child");

        child.start();
        shareData.set(shareData.get()+1);
        System.out.println("shareDate in main Thread:"+shareData.get());
    }

}
