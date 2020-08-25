package com.github.tocrhz.mqtt.subscriber;

import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * If {@link com.github.tocrhz.mqtt.annotation.NamedValue} is used, use regular matching,
 * if not, use {@link MqttTopic#isMatched(String, String)} matching
 *
 * @author tocrhz
 */
public class TopicPair {
    private final static Pattern TO_PATTERN = Pattern.compile("\\{(\\w+)}");
    private final static Pattern TO_TOPIC = Pattern.compile("[^/]*\\{\\w+}[^/]*");
    private final static String STRING_PARAM = "([^/]+)";
    private final static String NUMBER_PARAM = "(\\\\d+(:?\\\\.\\\\d+)?)";

    private String topic;
    private Pattern pattern;
    private String[] params;
    private int qos;

    public static TopicPair of(String topic, int qos, HashMap<String, Class<?>> paramTypeMap) {
        Assert.isTrue(topic != null && !topic.isEmpty(), "topic cannot be blank");
        Assert.isTrue(qos >= 0, "qos min value is 0");
        Assert.isTrue(qos <= 2, "qos max value is 2");
        TopicPair topicPair = new TopicPair();
        if (topic.contains("{")) {
            LinkedList<String> params = new LinkedList<>();
            topicPair.pattern = toPattern(topic, params, paramTypeMap);
            topicPair.params = params.toArray(new String[0]);
            topicPair.topic = TO_TOPIC.matcher(topic).replaceAll("+");
        } else {
            topicPair.topic = topic;
        }
        MqttTopic.validate(topicPair.topic, true);
        topicPair.qos = qos;
        return topicPair;
    }

    private static Pattern toPattern(String topic, LinkedList<String> params, HashMap<String, Class<?>> paramTypeMap) {
        String pattern = replaceSymbols(topic);
        Matcher matcher = TO_PATTERN.matcher(pattern);
        StringBuffer buffer = new StringBuffer("^");
        while (matcher.find()) {
            String paramName = matcher.group(1);
            params.add(paramName);
            if (paramTypeMap.containsKey(paramName)) {
                Class<?> paramType = paramTypeMap.get(paramName);
                if (Number.class.isAssignableFrom(paramType)) {
                    matcher.appendReplacement(buffer, NUMBER_PARAM);
                } else {
                    matcher.appendReplacement(buffer, STRING_PARAM);
                }
            } else {
                matcher.appendReplacement(buffer, STRING_PARAM);
            }
        }
        matcher.appendTail(buffer);
        buffer.append("$");
        return Pattern.compile(buffer.toString());
    }

    public String getTopic() {
        return topic;
    }

    public int getQos() {
        return qos;
    }

    public boolean isMatched(String topic) {
        if (this.pattern != null) {
            return pattern.matcher(topic).matches();
        } else {
            return MqttTopic.isMatched(this.topic, topic);
        }
    }

    public HashMap<String, String> getPathValueMap(String topic) {
        HashMap<String, String> map = new HashMap<>();
        if (pattern != null) {
            Matcher matcher = pattern.matcher(topic);
            if (matcher.find()) {
                for (int i = 0; i < params.length; i++) {
                    String group = matcher.group(i + 1);
                    map.put(params[i], group);
                }
            }
        }
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicPair topicPair = (TopicPair) o;
        return Objects.equals(topic, topicPair.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic);
    }

    public int order() {
        return this.pattern == null ? 1 : -params.length;
    }

    private static String replaceSymbols(String topic) {
        StringBuilder sb = new StringBuilder();
        char[] chars = topic.toCharArray();
        for (char ch : chars) {
            switch (ch) {
                case '$':
                case '^':
                case '.':
                case '?':
                case '*':
                case '|':
                case '(':
                case ')':
                case '[':
                case ']':
                case '\\':
                    sb.append('\\').append(ch);
                    break;
                case '+':
                    sb.append("[^/]+");
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }
}
