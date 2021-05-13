import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
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
}
