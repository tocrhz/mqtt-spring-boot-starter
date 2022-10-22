# mqtt-spring-boot-starter

MQTT starter for Spring Boot, easier to use.

> Support spring boot version: 2.x
>
> This document is machine translated.

## 0. 修改记录

2022-02-22 `v1.2.8`
1. 修复对象与字节转换的bug, 优先使用自定义的转换类，如果无法转换，再使用Spring的转换类
2. 新增内置String与对象互转的转换类，使用Jackson, 内置转换类不再使用匿名类
3. 修复一个配置的bug

2022-02-22 `v1.2.7`
1. 配置调整, `mqtt.clients` 中明确配置的 `client-id` 优先级更高

...

## 1. import

```xml
<dependency>
    <groupId>com.github.tocrhz</groupId>
    <artifactId>mqtt-spring-boot-starter</artifactId>
    <version>1.2.8</version>
</dependency>
```

## 2. properties

Most of the configuration has default values, they all start with 'mqtt.'.
Support multiple client.

e.g.

```properties

mqtt.uri=tcp://127.0.0.1:1883
mqtt.client-id=default_client
mqtt.username=username
mqtt.password=password

# mqtt.clients 中明确配置的 client-id 优先级更高
mqtt.clients.multi_client_1.client-id=new_client
mqtt.clients.multi_client_1.uri=tcp://127.0.0.1:1883
mqtt.clients.multi_client_1.username=username
mqtt.clients.multi_client_1.password=password

mqtt.clients.multi_client_2.uri=tcp://127.0.0.1:1883
mqtt.clients.multi_client_2.username=username
mqtt.clients.multi_client_2.password=password

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
     * clientId = multi_client_1
     * topic = test/+
     */
    @MqttSubscribe(value = "test/+", clients = "multi_client_1")
    public void sub(String topic, MqttMessage message, @Payload String payload) {
        logger.info("receive from    : {}", topic);
        logger.info("message payload : {}", new String(message.getPayload(), StandardCharsets.UTF_8));
        logger.info("string payload  : {}", payload);
    }

    /**
     * subscribe = $queue/test/+
     * topic = test/+
     * pattern = ^test/([^/]+)$
     */
    @MqttSubscribe(value="test/{id}", shared=true)
    public void sub(String topic, @NamedValue("id") String id, @Payload UserInfo userInfo) {
        logger.info("receive from   : {}", topic);
        logger.info("named value id : {}", id);
        logger.info("object payload : {}", userInfo);
    }

    /**
     * subscribe = $share/gp/test/+
     * topic = test/+
     * pattern = ^test/([^/]+)$
     */
    @MqttSubscribe(value="test/{id}", shared=true, groups="gp")
    public void sub(String topic, @NamedValue("id") String id, @Payload UserInfo userInfo) {
        logger.info("receive from   : {}", topic);
        logger.info("named value id : {}", id);
        logger.info("object payload : {}", userInfo);
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
        publisher.send("test/send", "test message, default QOS is 0.");
        publisher.send("test/send", "Specify QOS as 1.", 1);
        publisher.send("test/send", "Specify QOS as 2.", 2, false);
        publisher.send("multi_client_1", "test/send", "test message, default QOS is 0.");
    }
}
```

## 4. extension point.

#### payload serialize or deserialize

Implements `PayloadSerialize` and `PayloadDeserialize`, or implements `ConverterFactory` and `Converter` is the same.


```java

@Component
public class JacksonPayloadSerialize implements PayloadSerialize {
    private final static Logger log = LoggerFactory.getLogger(JacksonPayloadDeserialize.class);

    private final ObjectMapper objectMapper;

    public JacksonPayloadSerialize(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] convert(Object source) {
        try {
            return objectMapper.writeValueAsBytes(source);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Payload serialize error: {}", e.getMessage(), e);
        }
        return null;
    }
}

@Component
public class JacksonPayloadDeserialize implements PayloadDeserialize {
    private final static Logger log = LoggerFactory.getLogger(JacksonPayloadDeserialize.class);

    private final ObjectMapper objectMapper;

    public JacksonPayloadDeserialize(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Converter<byte[], T> getConverter(Class<T> targetType) {
        return source -> {
            try {
                if (targetType == String.class) {
                    return (T) new String(source, StandardCharsets.UTF_8);
                }
                return objectMapper.readValue(source, targetType);
            } catch (IOException e) {
                log.warn("Payload deserialize error: {}", e.getMessage(), e);
            }
            return null;
        };
    }
}
```

#### 配置

通过 `MqttConfigurer` 抽象类, 可以在创建客户端前, 连接前, 订阅前自定义操作.

```java
@Component
public class MyMqttConfigurer extends MqttConfigurer { 
    
    /**
     * 在创建客户端之前, 增删改客户端配置.
     * <p>清除的原有客户端, 增加客户端 "client01" </p>
     */
    public void beforeCreate(ClientRegistry registry) {
        registry.clear();
        registry.add("client01", "tcp://localhost:1883");
    }


    /**
     * 创建客户端.
     *
     * @param clientId 客户端ID
     * @param options  MqttConnectOptions
     */
    public IMqttAsyncClient postCreate(String clientId, MqttConnectOptions options) throws MqttException {
        return new MqttAsyncClient(options.getServerURIs()[0], clientId, new MemoryPersistence());
    }

    /**
     * 在创建客户端后, 订阅主题前, 修改订阅的主题.
     * <p>清除 client01 的原有订阅, 增加订阅 "/test/abc"</p>
     * 
     */
    public void beforeSubscribe(String clientId, Set<TopicPair> topicPairs) {
        if("client01".equals(clientId)){
            topicPairs.clear();
            topicPairs.add(TopicPair.of("/test/abc", 0));
        }
    }
}
```



