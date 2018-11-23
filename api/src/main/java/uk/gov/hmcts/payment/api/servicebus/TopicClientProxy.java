package uk.gov.hmcts.payment.api.servicebus;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
public class TopicClientProxy {

    @Autowired
    private WebApplicationContext applicationContext;

    public void send(IMessage message) throws InterruptedException, ServiceBusException {


        TopicClient topicClient = applicationContext.getBean(TopicClient.class);
        topicClient.send(message);
        topicClient.close();
    }

    public void close() throws ServiceBusException {
    }

}
