import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import com.github.tocrhz.mqtt.subscriber.TopicPair;


/**
 * @author tjheiska
 */
public class PatternTests {

    @Test
    public void testTopicPair()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            SecurityException {
        HashMap<String, Class<?>> paramTypeMap = new HashMap<>();
        paramTypeMap.put("projectId", Long.class);
        paramTypeMap.put("userName", String.class);

        TopicPair topicPair = TopicPair.of("{projectId}/{userName}", 0, false, null, paramTypeMap);
        String topic = "0/user1";

        assert (topicPair.isMatched(topic));
        HashMap<String, String> map = topicPair.getPathValueMap(topic);
        assertEquals("0", map.get("projectId"));
        assertEquals("user1", map.get("userName"));
    }

    @Test
    public void testHashTagPattern()
      throws NoSuchFieldException, SecurityException, IllegalArgumentException,
          IllegalAccessException {
        final String topicFilter = "my/hierarchical/topics/{param}/#";
        HashMap<String, Class<?>> paramTypeMap = new HashMap<>();
        paramTypeMap.put("param", String.class);
        TopicPair topicPair = TopicPair.of(topicFilter, 0, false, null, paramTypeMap);
        final Field patternField = TopicPair.class.getDeclaredField("pattern");
        patternField.setAccessible(true);
        final Pattern pattern = (Pattern) patternField.get(topicPair);
        final String testTopic = "my/hierarchical/topics/one/with/subtopics";
        assertEquals("^my/hierarchical/topics/([^/]+)/.*$", pattern.pattern());
        assert (pattern.matcher(testTopic).matches());
        assert (topicPair.isMatched(testTopic));
    }

    @Test
    public void testMatchAll() {
        TopicPair topicPair = TopicPair.of("#", 0, false, null, new HashMap<>());
        assert (topicPair.isMatched("this/should/be/matched"));
    }

}
