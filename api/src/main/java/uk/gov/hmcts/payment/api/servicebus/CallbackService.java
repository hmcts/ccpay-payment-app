package uk.gov.hmcts.payment.api.servicebus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class CallbackService {

    @Autowired
    private PaymentDtoMapper paymentDtoMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private TopicClient topicClient;

    public void callback(PaymentFeeLink payment) {

        PaymentDto dto = paymentDtoMapper.toRetrieveCardPaymentResponseDto(payment);

        try {

            Message msg = new Message(objectMapper.writeValueAsString(dto));

            msg.setContentType("application/json");
            msg.setLabel("Service Callback Message");
            msg.setProperties(Collections.singletonMap("serviceCallbackUrl", payment.getPayments().get(0).getServiceCallbackUrl()));

            topicClient.send(msg);
            topicClient.close();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ServiceBusException e) {
            e.printStackTrace();
        }

    }

    public void callbackAmpq(PaymentFeeLink payment) {

        String url = payment.getPayments().get(0).getServiceCallbackUrl();

        PaymentDto dto = paymentDtoMapper.toRetrieveCardPaymentResponseDto(payment);

        ProducerTemplate template = camelContext.createProducerTemplate();
        template.setDefaultEndpointUri("amqp://serviceCallbackTopic");

        Map<String, Object> headers = new HashMap<>();
        headers.put("serviceCallbackUrl", url);

        try {
            template.sendBody(
                objectMapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}
