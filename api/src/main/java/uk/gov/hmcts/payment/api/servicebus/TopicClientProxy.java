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

    private static final int MESSAGE_SEND_MAX_RETRY_COUNT = 3;

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
        int attempt = 0;
        while (attempt < MESSAGE_SEND_MAX_RETRY_COUNT) {
            try {
                client.send(message);
                break; // Success
            } catch (ServiceBusException | InterruptedException e) {
                attempt++;
                LOG.warn("Send attempt {} failed: {}", attempt, e.getMessage());
                if (attempt >= MESSAGE_SEND_MAX_RETRY_COUNT) {
                    LOG.error("All send attempts failed", e);
                    throw e;
                }
                Thread.sleep(1000L * attempt); // Exponential backoff
            }
        }
    }

    TopicClient newTopicClient() throws ServiceBusException, InterruptedException {
        ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(connectionString, topic);
        return new TopicClient(connectionStringBuilder);
    }

    public synchronized void send(IMessage message) throws InterruptedException, ServiceBusException {
        LOG.info("About to send message to topic: {}", topic);
        try {
            if (!keepClientAlive) {
                LOG.info("Connection String Alive: {}", connectionString);
                TopicClient client = newTopicClient();
                LOG.info("Created new TopicClient");
                send(client, message);
                LOG.info("Message sent, closing client");
                client.close();
                LOG.info("Client closed");
                return;
            }
            if (topicClient == null) {
                LOG.info("Connection String Not Alive: {}", connectionString);
                topicClient = newTopicClient();
                LOG.info("Created persistent TopicClient");
            }
            send(topicClient, message);
            LOG.info("Message sent using persistent client");
        } catch (Exception e) {
            LOG.error("Error sending message to topic", e);
            throw e;
        }
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
