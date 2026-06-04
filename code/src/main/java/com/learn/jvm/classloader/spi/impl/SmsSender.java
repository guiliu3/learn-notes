package com.learn.jvm.classloader.spi.impl;

import com.learn.jvm.classloader.spi.MessageSender;

/**
 * SPI 实现二：短信发送
 * 与 EmailSender 同理，不同厂商提供不同实现，框架层代码无需修改
 */
public class SmsSender implements MessageSender {

    @Override
    public void send(String message) {
        System.out.println("[SMS] 发送短信：" + message);
    }

    @Override
    public String getType() {
        return "SMS";
    }
}
