package com.github.tocrhz.mqtt.subscriber;

@FunctionalInterface
public interface IMessageHandler {
    void receive(Object[] parameters) throws Exception;
}
