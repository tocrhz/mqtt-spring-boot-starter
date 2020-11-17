package com.github.tocrhz.mqtt.subscriber;

import com.github.tocrhz.mqtt.annotation.MqttSubscribe;
import com.github.tocrhz.mqtt.autoconfigure.MqttConversionService;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Used to subscribe message
 *
 * @author tocrhz
 */
public class MqttSubscriber {
    private final static Logger log = LoggerFactory.getLogger(MqttSubscriber.class);

    public void accept(String clientId, String topic, MqttMessage mqttMessage) {
        Optional<TopicPair> matched = matched(clientId, topic);
        if (matched.isPresent()) {
            try {
                method.invoke(bean, fillParameters(matched.get(), topic, mqttMessage));
            } catch (Throwable throwable) {
                log.error("message handler error: {}", throwable.getMessage(), throwable);
            }
        }
    }

    private String[] clientIds;
    private Object bean;
    private Method method;
    private LinkedList<ParameterModel> parameters;
    private int order;
    private boolean signPayload = false;

    private final LinkedList<TopicPair> topics = new LinkedList<>();

    public static MqttSubscriber of(Object bean, Method method) {
        MqttSubscriber subscriber = new MqttSubscriber();
        subscriber.bean = bean;
        subscriber.method = method;
        subscriber.parameters = ParameterModel.of(method);
        subscriber.signPayload = subscriber.parameters.stream().anyMatch(ParameterModel::isSign);
        HashMap<String, Class<?>> paramTypeMap = new HashMap<>();
        subscriber.parameters.stream()
                .filter(model -> model.getName() != null)
                .forEach(model -> paramTypeMap.put(model.getName(), model.getType()));
        if (method.isAnnotationPresent(Order.class)) {
            Order order = method.getAnnotation(Order.class);
            subscriber.order = order.value();
        }
        MqttSubscribe subscribe = method.getAnnotation(MqttSubscribe.class);
        subscriber.clientIds = subscribe.clients();
        subscriber.setTopics(subscribe, paramTypeMap);
        return subscriber;
    }


    private void setTopics(MqttSubscribe subscribe, HashMap<String, Class<?>> paramTypeMap) {
        String[] topics = subscribe.value();
        int[] qos = fillQos(topics, subscribe.qos());
        boolean[] shared = fillShared(topics, subscribe.shared());
        String[] groups = fillGroups(topics, subscribe.groups());
        LinkedHashSet<TopicPair> temps = new LinkedHashSet<>();
        for (int i = 0; i < topics.length; i++) {
            temps.add(TopicPair.of(topics[i], qos[i], shared[i], groups[i], paramTypeMap));
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

    private boolean[] fillShared(String[] topics, boolean[] shared) {
        int topic_len = topics.length;
        int qos_len = shared.length;
        if (topic_len > qos_len) {
            boolean[] temp = new boolean[topic_len];
            System.arraycopy(shared, 0, temp, 0, qos_len);
            Arrays.fill(temp, qos_len, topic_len, shared[qos_len - 1]);
            return temp;
        } else if (qos_len > topic_len) {
            boolean[] temp = new boolean[topic_len];
            System.arraycopy(shared, 0, temp, 0, topic_len);
            return temp;
        }
        return shared;
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
            } else if (parameter.isSign() && mqttMessage != null) {
                value = MqttConversionService.getSharedInstance().fromBytes(mqttMessage.getPayload(), target, converters);
            } else if (name != null && pathValueMap.containsKey(name)) {
                value = fromTopic(pathValueMap.get(name), target);
            } else if (target == String.class) {
                value = topic;
            } else if (!signPayload && target.getClassLoader() != null && mqttMessage != null) {
                value = MqttConversionService.getSharedInstance().fromBytes(mqttMessage.getPayload(), target, converters);
            }
            if (value == null) {
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

    public int getOrder() {
        return order;
    }

    public LinkedList<TopicPair> getTopics() {
        return topics;
    }

    public boolean contains(String clientId) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MqttSubscriber that = (MqttSubscriber) o;
        return Objects.equals(bean, that.bean) &&
                Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bean, method);
    }
}
