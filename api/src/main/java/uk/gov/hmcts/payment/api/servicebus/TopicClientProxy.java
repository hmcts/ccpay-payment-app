package uk.gov.hmcts.payment.api.servicebus;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class TopicClientProxy {

    private static final Logger LOG = LoggerFactory.getLogger(TopicClientProxy.class);

    private static final int MESSAGE_SEND_MAX_RETRY_COUNT = 3;
    private static final String HEADER_SIGNATURE = "X-Message-Signature";
    private static final String HEADER_SENDER = "X-Sender-Service";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String SIGNATURE_VERSION = "v1";
    private static final String SENDER_SERVICE = "ccpay-payment";

    private final String connectionString;

    private final String topic;

    private final String hmacSecret;

    private boolean keepClientAlive = false;

    private TopicClient topicClient;

    public TopicClientProxy(String connectionString, String topic) {
        this(connectionString, topic, null);
    }

    @Autowired
    public TopicClientProxy(
        @Value("${azure.servicebus.connection-string}") String connectionString,
        @Value("${azure.servicebus.topic-name}") String topic,
        @Value("${ccpay.message.signing.key}") String hmacSecret) {
        this.connectionString = connectionString;
        this.topic = topic;
        this.hmacSecret = hmacSecret;
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

    private void signMessage(IMessage message) {
        if (hmacSecret == null || hmacSecret.isBlank()) {
            throw new IllegalStateException("ccpay.message.signing.key must be configured");
        }

        String timestamp = Instant.now().toString();
        Map<String, String> properties = new HashMap<>();
        if (message.getProperties() != null) {
            properties.putAll(message.getProperties());
        }

        properties.put(HEADER_SENDER, SENDER_SERVICE);
        properties.put(HEADER_TIMESTAMP, timestamp);
        properties.put(HEADER_SIGNATURE, hmacSha256Base64(buildPayloadToSign(message, timestamp), hmacSecret));
        message.setProperties(properties);
    }

    String buildPayloadToSign(IMessage message, String timestamp) {
        return String.join("|",
            SIGNATURE_VERSION,
            SENDER_SERVICE,
            timestamp,
            message.getLabel() == null ? "" : message.getLabel(),
            message.getContentType() == null ? "" : message.getContentType(),
            Base64.getEncoder().encodeToString(message.getBody())
        );
    }

    String hmacSha256Base64(String payload, String base64Secret) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(base64Secret);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to calculate HMAC-SHA256", e);
        }
    }

    public synchronized void send(IMessage message) throws InterruptedException, ServiceBusException {
        LOG.info("About to send message to topic: {}", topic);
        try {
            signMessage(message);
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
