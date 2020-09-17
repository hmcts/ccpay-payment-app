package uk.gov.hmcts.payment.api.servicebus;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TopicClientProxy {

    private static final Logger LOG = LoggerFactory.getLogger(TopicClientProxy.class);

    private final String connectionString;

    private final String topic;

    private boolean keepClientAlive = false;

    private TopicClient topicClient;

    public TopicClientProxy(
        @Value("${azure.servicebus.connection-string}") String connectionString,
        @Value("${azure.servicebus.topic-name}") String topic) {

        this.connectionString = connectionString;
        this.topic = topic;
    }


    private void send(TopicClient client, IMessage message) throws InterruptedException, ServiceBusException {
        LOG.info("Azure Service-Bus Connection: {}", connectionString);
        LOG.info("Azure Service-Bus Topic: {}", topic);

        client.send(message);
    }

    private TopicClient newTopicClient() throws ServiceBusException, InterruptedException {
        ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(connectionString, topic);
        return new TopicClient(connectionStringBuilder);
    }

    public synchronized void send(IMessage message) throws InterruptedException, ServiceBusException {

        LOG.info("Azure Service-Bus Connection: {}", connectionString);
        LOG.info("Azure Service-Bus Topic: {}", topic);

        if (!keepClientAlive) { /* One use client */

            TopicClient client = newTopicClient();
            send(client, message);
            client.close();
            return;
        }

        /* Batch mode */

        if (topicClient == null) {
            topicClient = newTopicClient();
        }

        send(topicClient, message);

    }

    public void close() {

        if (topicClient == null) {
            return;
        }

        try {
            topicClient.close();
        } catch (ServiceBusException e) {
            LOG.error("Error closing topic client", e);
        }

        topicClient = null;
    }

    public synchronized void setKeepClientAlive(boolean keepClientAlive) {
        this.keepClientAlive = keepClientAlive;
    }

}
