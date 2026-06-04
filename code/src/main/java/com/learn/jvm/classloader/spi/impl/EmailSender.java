package com.learn.jvm.classloader.spi.impl;

import com.learn.jvm.classloader.spi.MessageSender;

/**
 * SPI 实现一：邮件发送（模拟 MySQL Driver 在 classpath 下的角色）
 * 实现类打包在具体的 jar 里，通过 META-INF/services 声明
 */
public class EmailSender implements MessageSender {

    @Override
    public void send(String message) {
        System.out.println("[Email] 发送邮件：" + message);
    }

    @Override
    public String getType() {
        return "Email";
    }
}
