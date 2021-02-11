package uk.gov.hmcts.payment.api.servicebus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.Message;
import org.ff4j.FF4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.CallbackService;

import java.util.Collections;


@Service
public class CallbackServiceImpl implements CallbackService {

    private final PaymentDtoMapper paymentDtoMapper;

    private final ObjectMapper objectMapper;

    private final TopicClientProxy topicClient;

    private final FF4j ff4j;

    @Autowired
    public CallbackServiceImpl(PaymentDtoMapper paymentDtoMapper, ObjectMapper objectMapper, TopicClientProxy topicClient, FF4j ff4j) {
        this.paymentDtoMapper = paymentDtoMapper;
        this.objectMapper = objectMapper;
        this.topicClient = topicClient;
        this.ff4j = ff4j;
    }

    private static final Logger LOG = LoggerFactory.getLogger(CallbackServiceImpl.class);

    public synchronized void callback(PaymentFeeLink paymentFeeLink, Payment payment) {

        if (payment.getServiceCallbackUrl() == null) {
            LOG.warn("Service callback url is null");
            return;
        }

        if (!ff4j.check(CallbackService.FEATURE)) {
            LOG.warn("Service callback feature is disabled");
            return;
        }

        PaymentDto dto = paymentDtoMapper.toResponseDto(paymentFeeLink, payment);

        try {

            Message msg = new Message(objectMapper.writeValueAsString(dto));

            msg.setContentType("application/json");
            msg.setLabel("Service Callback Message");
            msg.setProperties(Collections.singletonMap("serviceCallbackUrl", payment.getServiceCallbackUrl()));

            topicClient.send(msg);

        } catch (Exception e) {
            LOG.error("Error", e);
        }

    }

}
