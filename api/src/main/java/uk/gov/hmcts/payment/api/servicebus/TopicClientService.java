package uk.gov.hmcts.payment.api.servicebus;

import com.microsoft.azure.servicebus.primitives.ServiceBusException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class TopicClientService {

    @Value("${azure.servicebus.connection-string}")
    private String connectionString;

    @Value("${ccpay.message.signing.key}")
    private String hmacSecret;

    String topic = "ccpay-service-request-cpo-update-topic";

    public TopicClientProxy getTopicClientProxy() throws ServiceBusException, InterruptedException {
        return new TopicClientProxy(connectionString, topic, hmacSecret);
    }

}

