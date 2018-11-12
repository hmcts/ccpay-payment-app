package uk.gov.hmcts.payment.api.servicebus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

@Service
public class CallbackService {

    @Autowired
    private PaymentDtoMapper paymentDtoMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CamelContext camelContext;

    public void callback(PaymentFeeLink payment) {

        String url = payment.getPayments().get(0).getServiceCallbackUrl();

        PaymentDto dto = paymentDtoMapper.toRetrieveCardPaymentResponseDto(payment);

        ProducerTemplate template = camelContext.createProducerTemplate();
        template.setDefaultEndpointUri("amqp://serviceCallbackTopic");
        try {
            template.sendBodyAndHeader(
                objectMapper.writeValueAsString(dto),
                "service-callback-url:", url);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}
