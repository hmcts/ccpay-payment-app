package uk.gov.hmcts.payment.api.servicebus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.Message;
import org.ff4j.FF4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
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

    private final PaymentGroupDtoMapper paymentGroupDtoMapper;

    @Autowired
    public CallbackServiceImpl(PaymentDtoMapper paymentDtoMapper, ObjectMapper objectMapper,
                               TopicClientProxy topicClient, FF4j ff4j, @Lazy  PaymentGroupDtoMapper paymentGroupDtoMapper) {
        this.paymentDtoMapper = paymentDtoMapper;
        this.objectMapper = objectMapper;
        this.topicClient = topicClient;
        this.ff4j = ff4j;
        this.paymentGroupDtoMapper = paymentGroupDtoMapper;
    }

    private static final Logger LOG = LoggerFactory.getLogger(CallbackServiceImpl.class);

    public synchronized void callback(PaymentFeeLink paymentFeeLink, Payment payment) {

        if (!ff4j.check(CallbackService.FEATURE)) {
            LOG.warn("Service callback feature is disabled");
            return;
        }

        if (null != payment.getServiceCallbackUrl()) {
            try {
                PaymentDto dto = paymentDtoMapper.toResponseDto(paymentFeeLink, payment);
                LOG.info("PaymentDto: {}", dto);

                Message msg = new Message(objectMapper.writeValueAsString(dto));

                msg.setContentType("application/json");
                msg.setLabel("Service Callback Message");
                msg.setProperties(Collections.singletonMap("serviceCallbackUrl", payment.getServiceCallbackUrl()));

                topicClient.send(msg);

            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        } else if (null != paymentFeeLink.getCallBackUrl()) {
            try {
                String serviceRequestStatus =
                        paymentGroupDtoMapper.toPaymentGroupDto(paymentFeeLink).getServiceRequestStatus();
                PaymentStatusDto paymentStatusDto =
                        paymentDtoMapper.toPaymentStatusDto(paymentFeeLink.getPaymentReference(), "", payment,
                                serviceRequestStatus);
                LOG.info("PaymentStatusDto: {}", paymentStatusDto);
                Message msg = new Message(objectMapper.writeValueAsString(paymentStatusDto));

                msg.setContentType("application/json");
                msg.setLabel("Service Callback Message");
                msg.setProperties(Collections.singletonMap("serviceCallbackUrl", paymentFeeLink.getCallBackUrl()));

                topicClient.send(msg);

            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
