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
@SuppressWarnings("unused")
public class TopicPair {
    private final static Pattern TO_PATTERN = Pattern.compile("\\{(\\w+)}");
    private final static Pattern TO_TOPIC = Pattern.compile("[^/]*\\{\\w+}[^/]*");
    private final static String STRING_PARAM = "([^/]+)";
    private final static String NUMBER_PARAM = "(\\\\d+(:?\\\\.\\\\d+)?)";

    private String topic;
    private Pattern pattern;
    private TopicParam[] params;
    private int qos;
    private String group;

    public static TopicPair of(String topic, int qos) {
        return of(topic, qos, null, new HashMap<>());
    }

    public static TopicPair of(String topic, int qos, String group, HashMap<String, Class<?>> paramTypeMap) {
        Assert.isTrue(topic != null && !topic.isEmpty(), "topic cannot be blank");
        Assert.isTrue(qos >= 0, "qos min value is 0");
        Assert.isTrue(qos <= 2, "qos max value is 2");
        TopicPair topicPair = new TopicPair();
        if (topic.contains("{")) {
            LinkedList<TopicParam> params = new LinkedList<>();
            topicPair.pattern = toPattern(topic, params, paramTypeMap);
            topicPair.params = params.toArray(new TopicParam[0]);
            topicPair.topic = TO_TOPIC.matcher(topic).replaceAll("+");
        } else {
            topicPair.topic = topic;
        }
        MqttTopic.validate(topicPair.topic, true);
        topicPair.qos = qos;
        topicPair.group = group;
        return topicPair;
    }

    private static Pattern toPattern(String topic, LinkedList<TopicParam> params, HashMap<String, Class<?>> paramTypeMap) {
        String pattern = replaceSymbols(topic);
        Matcher matcher = TO_PATTERN.matcher(pattern);
        StringBuffer builder = new StringBuffer("^");
        int group = 1;
        while (matcher.find()) {
            String paramName = matcher.group(1);
            params.add(new TopicParam(paramName, group));
            if (paramTypeMap.containsKey(paramName)) {
                Class<?> paramType = paramTypeMap.get(paramName);
                if (Number.class.isAssignableFrom(paramType)) {
                    matcher.appendReplacement(builder, NUMBER_PARAM);
                    ++group;
                } else {
                    matcher.appendReplacement(builder, STRING_PARAM);
                }
            } else {
                matcher.appendReplacement(builder, STRING_PARAM);
            }
            ++group;
        }
        matcher.appendTail(builder);
        builder.append("$");
        return Pattern.compile(builder.toString());
    }

    public String getTopic(boolean enableShare) {
        if (enableShare) {
            if (this.group != null && !this.group.isEmpty()) {
                return "$share/" + this.group + "/" + this.topic;
            }
        }
        return this.topic;
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
                for (TopicParam param : params) {
                    String group = matcher.group(param.getAt());
                    map.put(param.getName(), group);
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
                case '#':
                    sb.append(".*");
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }
}
