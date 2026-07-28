package com.learn.thread;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 演示父子线程，用自己传递值给子线程。
 */
public class ShareDataParentThreadDemoForSelf {

    public static void main(String[] args) throws InterruptedException {

        ConcurrentHashMap<String,String> shareDate = new ConcurrentHashMap<>();

        MyThread thread = new MyThread(shareDate);
        thread.start();
        shareDate.put("key","value");

        Thread.sleep(1000L);
        System.out.println("shareData in main Thread:"+shareDate.get("key"));

    }



}

class MyThread extends  Thread{
  ConcurrentHashMap<String,String> shareDate;

  public MyThread(ConcurrentHashMap data){
      this.shareDate = data;
  }

  public void run(){
      shareDate.put("key","new vlaue");
      System.out.println("share Data in child thread :"+shareDate.get("key"));
  }

}