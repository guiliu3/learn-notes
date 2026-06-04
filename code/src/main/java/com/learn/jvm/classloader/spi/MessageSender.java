package com.learn.jvm.classloader.spi;

/**
 * SPI 接口（模拟 java.sql.Driver 在 rt.jar 中的角色）
 * 接口由"框架层"定义，实现由"用户/厂商"提供
 */
public interface MessageSender {

    /**
     * 发送消息
     * @param message 消息内容
     */
    void send(String message);

    /**
     * 获取发送方式名称
     */
    String getType();
}
