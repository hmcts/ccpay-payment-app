package uk.gov.hmcts.payment.api.servicebus;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TopicClientProxy {

    @Value("${azure.servicebus.connection-string}")
    private String connectionString;

    @Value("${azure.servicebus.topic-name}")
    private String topic;

    public void send(IMessage message) throws InterruptedException, ServiceBusException {

        ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(connectionString, topic);

        TopicClient topicClient = new TopicClient(connectionStringBuilder);

        topicClient.send(message);
        topicClient.close();
    }

}
