package uk.gov.hmcts.payment.api.servicebus;

import com.microsoft.azure.servicebus.Message;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.servicebus.TopicClientProxy;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
public class TopicClientProxyTest {

    @Value("${azure.servicebus.connection-string}")
    private String connectionString;

    @Value("${azure.servicebus.topic-name}")
    private String topic;

    @Test
    public void testBatchMode() throws Exception{

        TopicClientProxy topicClientProxy = new TopicClientProxy(
            connectionString, topic
        );

        topicClientProxy.setKeepClientAlive(true);

        try{
            topicClientProxy.send(new Message("Hi!"));
        }catch(Exception e) {

        }

        topicClientProxy.close();

        topicClientProxy.setKeepClientAlive(false);

    }

}
