package com.github.tocrhz.mqtt.subscriber;

import com.github.tocrhz.mqtt.convert.MqttConversionService;
import com.github.tocrhz.mqtt.exception.NullParameterException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.convert.converter.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Used to subscribe message
 *
 * @author tocrhz
 */
public class MqttSubscriber {
    private final static Logger log = LoggerFactory.getLogger(MqttSubscriber.class);
    private static final LinkedList<MqttSubscriber> SUBSCRIBERS = new LinkedList<>();
    /**
     * 接收消息并处理
     *
     * @param clientId    接收当前消息的客户端ID
     * @param topic       当前消息的主题
     * @param mqttMessage 当前消息内容
     */
    public void accept(String clientId, String topic, MqttMessage mqttMessage) {
        Optional<TopicPair> matched = matched(clientId, topic);
        if (matched.isPresent()) {
            try {
                Object[] parameters = fillParameters(matched.get(), topic, mqttMessage);
                handler.receive(parameters);
            } catch (NullParameterException e) {
                log.debug("message params error: {}", e.getMessage());
            } catch (Exception e) {
                log.error("message handler error: {}", e.getMessage(), e);
            }
        }
    }

    private SubscriberModel subscribe;
    private String[] clientIds;
    private IMessageHandler handler;
    private LinkedList<ParameterModel> parameters;

    private final LinkedList<TopicPair> topics = new LinkedList<>();

    public static MqttSubscriber of(SubscriberModel subscribe, Object bean, Method method) {
        LinkedList<ParameterModel> parameters = ParameterModel.of(method);
        IMessageHandler handler = (params) -> method.invoke(bean, params);
        return of(subscribe, parameters, handler);
    }

    /**
     * 创建消息处理对象
     *
     * @param subscribe  订阅模型
     * @param parameters 处理方法的参数
     * @param handler    消息处理方法
     */
    public static MqttSubscriber of(SubscriberModel subscribe, LinkedList<ParameterModel> parameters, IMessageHandler handler) {
        MqttSubscriber subscriber = new MqttSubscriber();
        subscriber.subscribe = subscribe;
        subscriber.handler = handler;
        subscriber.parameters = parameters;
        return subscriber;
    }

    public static LinkedList<MqttSubscriber> list(){
        return SUBSCRIBERS;
    }
    public static void add(MqttSubscriber subscriber){
        SUBSCRIBERS.add(subscriber);
    }
    public static void destroy(){
        SUBSCRIBERS.clear();
    }


    private void setTopics(SubscriberModel subscribe, HashMap<String, Class<?>> paramTypeMap) {
        String[] topics = subscribe.value();
        int[] qos = fillQos(topics, subscribe.qos());
        String[] groups = fillGroups(topics, subscribe.groups());
        LinkedHashSet<TopicPair> temps = new LinkedHashSet<>();
        for (int i = 0; i < topics.length; i++) {
            temps.add(TopicPair.of(topics[i], qos[i], groups[i], paramTypeMap));
        }
        this.topics.addAll(temps);
        this.topics.sort(Comparator.comparingInt(TopicPair::order));
    }

    private int[] fillQos(String[] topics, int[] qos) {
        int topic_len = topics.length;
        int qos_len = qos.length;
        if (topic_len > qos_len) {
            int[] temp = new int[topic_len];
            System.arraycopy(qos, 0, temp, 0, qos_len);
            Arrays.fill(temp, qos_len, topic_len, qos[qos_len - 1]);
            return temp;
        } else if (qos_len > topic_len) {
            int[] temp = new int[topic_len];
            System.arraycopy(qos, 0, temp, 0, topic_len);
            return temp;
        }
        return qos;
    }

    private String[] fillGroups(String[] topics, String[] groups) {
        int topic_len = topics.length;
        int qos_len = groups.length;
        if (topic_len > qos_len) {
            String[] temp = new String[topic_len];
            System.arraycopy(groups, 0, temp, 0, qos_len);
            Arrays.fill(temp, qos_len, topic_len, groups[qos_len - 1]);
            return temp;
        } else if (qos_len > topic_len) {
            String[] temp = new String[topic_len];
            System.arraycopy(groups, 0, temp, 0, topic_len);
            return temp;
        }
        return groups;
    }

    private Optional<TopicPair> matched(final String clientId, final String topic) {
        if (clientIds == null || clientIds.length == 0
                || Arrays.binarySearch(clientIds, clientId) >= 0) {
            return topics.stream()
                    .filter(pair -> pair.isMatched(topic))
                    .findFirst();
        }
        return Optional.empty();
    }

    private Object[] fillParameters(TopicPair topicPair, String topic, MqttMessage mqttMessage) {
        HashMap<String, String> pathValueMap = topicPair.getPathValueMap(topic);
        LinkedList<Object> objects = new LinkedList<>();
        for (ParameterModel parameter : parameters) {
            Class<?> target = parameter.getType();
            String name = parameter.getName();
            LinkedList<Converter<Object, Object>> converters = parameter.getConverters();
            Object value = null;
            if (target == MqttMessage.class) {
                value = mqttMessage;
            } else if (parameter.isPayload() && mqttMessage != null) {
                value = MqttConversionService.getSharedInstance().fromBytes(mqttMessage.getPayload(), target, converters);
            } else if (name != null) {
                if (pathValueMap.containsKey(name)) {
                    value = fromTopic(pathValueMap.get(name), target);
                }
            } else if (target == String.class) {
                value = topic;
            } else if (target.getClassLoader() != null && mqttMessage != null) {
                value = MqttConversionService.getSharedInstance().fromBytes(mqttMessage.getPayload(), target, converters);
            }
            if (value == null) {
                if (parameter.isRequired()) {
                    throw new NullParameterException(parameter);
                }
                value = parameter.getDefaultValue();
            }
            objects.add(value);
        }
        return objects.toArray();
    }

    private Object fromTopic(String value, Class<?> target) {
        if (MqttConversionService.getSharedInstance()
                .canConvert(String.class, target)) {
            return MqttConversionService.getSharedInstance().convert(value, target);
        } else {
            log.warn("Unsupported covert from {} to {}", String.class.getName(), target.getName());
            return null;
        }
    }

    public LinkedList<TopicPair> getTopics() {
        return topics;
    }

    public boolean containsClientId(String clientId) {
        if (this.clientIds == null || this.clientIds.length == 0) {
            return true; // for all client
        }
        for (String id : clientIds) {
            if (id.equals(clientId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否已经填充参数了
     */
    private boolean hasResolveEmbeddedValue;

    /**
     * 主要用来填充spring的参数
     *
     * @param factory spring
     */
    public void resolveEmbeddedValue(ConfigurableBeanFactory factory) {
        if (hasResolveEmbeddedValue) {
            return;
        }
        hasResolveEmbeddedValue = true;
        if (factory != null) {
            String[] clients = subscribe.clients();
            for (int i = 0; i < clients.length; i++) {
                clients[i] = factory.resolveEmbeddedValue(clients[i]);
            }
            String[] value = subscribe.value();
            for (int i = 0; i < value.length; i++) {
                value[i] = factory.resolveEmbeddedValue(value[i]);
            }
        }

        HashMap<String, Class<?>> paramTypeMap = new HashMap<>();
        this.parameters.stream()
                .filter(param -> param.getName() != null)
                .forEach(param -> paramTypeMap.put(param.getName(), param.getType()));
        this.clientIds = subscribe.clients();
        this.setTopics(subscribe, paramTypeMap);
    }
}
