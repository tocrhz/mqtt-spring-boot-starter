package com.github.tocrhz.mqtt.subscriber;

/**
 * 消息处理方法接口定义.
 */
@FunctionalInterface
public interface IMessageHandler {
    /**
     * 处理方法
     *
     * @param parameters 参数表
     * @throws Exception 可能的异常
     */
    void receive(Object[] parameters) throws Exception;
}
