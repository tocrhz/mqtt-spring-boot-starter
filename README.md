# mqtt-spring-boot-starter

MQTT starter for Spring Boot, easier to use.

> Support spring boot version: 1.5.x ~ 2.3.x
>
> This document is machine translated, forgive me.


## 1. import

```xml
<dependency>
    <groupId>com.github.tocrhz</groupId>
    <artifactId>mqtt-spring-boot-starter</artifactId>
    <version>1.0.0.RELEASE</version>
</dependency>
```

## 2. properties

Most of the configuration has default values, they all start with 'mqtt.'.

e.g.

```properties
mqtt.uri=tcp://127.0.0.1:1883
mqtt.client-id=test_client
```

## 3. usage

#### subscribe

Add @MqttSubscribe annotation to any public method.

e.g.

```java
@Component
public class MqttMessageHandler {
    
    /**
     * topic = test/+
     */
    @MqttSubscribe("test/+")
    public void sub(String topic, MqttMessage message, @Payload String payload) {
        logger.info("receive from    : {}", topic);
        logger.info("message payload : {}", new String(message.getPayload(), StandardCharsets.UTF_8));
        logger.info("string payload  : {}", payload);
    }

    /**
     * topic = test/+
     * pattern = ^test/([^/]+)$
     */
    @MqttSubscribe("test/{id}")
    public void sub(String topic, @Named("id") String id, @Payload UserInfo userInfo) {
        logger.info("receive from   : {}", topic);
        logger.info("named value id : {}", payload);
        logger.info("object payload : {}", payload);
    }

}
```

#### publish

Just inject `MqttPublisher` and call the `send` method.

e.g.

```java
@Component
public class DemoService {

    private final MqttPublisher publisher;

    public DemoService(MqttPublisher publisher) {
        this.publisher = publisher;
    }

    public void sendTest(){
        publisher.send("test/send", "test message, default QOS is 1.");
        publisher.send("test/send", "Specify QOS as 0.", 0);
        publisher.send("test/send", "Specify QOS as 2.", 2, false);
    }
}
```

## 4. extension point

#### payload serialize or deserialize

Extend `PayloadAutoConfiguration` and implement methods.
Or implement the `PayloadSerialize` and `PayloadDeserialize` interfaces respectively.

e.g.

```java
@Configuration
public class MqttPayloadConfig extends PayloadAutoConfiguration {

    // jackson ObjectMapper.
    private final ObjectMapper objectMapper;

    public MqttPayloadConfig(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }

    @Override
    protected void registry(ConverterRegistry registry) {
        
        // serialize
        registry.addConverter((PayloadSerialize) source -> objectMapper.writeValueAsBytes(source));
        
        // deserialize
        registry.addConverterFactory(new PayloadDeserialize() {
            @Override
            public <T> Converter<byte[], T> getConverter(Class<T> targetType) {
                return source -> objectMapper.readValue(source, targetType);
            }
        });
    }
}
```

#### client persistence

Any implementation class of the `MqttClientPersistence` interface, injected into the spring container.

default is `MemoryPersistence`.

```java
@Configuration
public class MqttAutoConfiguration {
    // ... 
    @Bean
    @Order(1010)
    @ConditionalOnMissingBean(MqttClientPersistence.class)
    public MqttClientPersistence mqttClientPersistence() {
        return new MemoryPersistence();
    }
    // ... 
}
```


#### ssl or other

Extend `MqttConnectOptionsAdapter` and implement methods.

e.g.

```java
@Component
public class MqttSslConfiguration extends MqttConnectOptionsAdapter { 
    
    protected void configure(MqttConnectOptions options) {
        // ssl
        options.setSocketFactory(SSLSocketFactory.getDefault());
    }
}
```


