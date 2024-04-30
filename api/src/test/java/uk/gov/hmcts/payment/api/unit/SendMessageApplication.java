package uk.gov.hmcts.payment.api.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.Message;
import uk.gov.hmcts.payment.api.componenttests.CardPaymentComponentTest;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.servicebus.TopicClientProxy;

import java.util.Arrays;
import java.util.Collections;

public class SendMessageApplication {


    public static void main(String[] args) throws Exception{

        TopicClientProxy client = new TopicClientProxy("<Service-bus Connection String>", "serviceCallbackTopic");

        String paymentReference = "00000005";

        Payment payment = CardPaymentComponentTest.getPaymentsData().get(2);

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference(paymentReference)
            .payments(Arrays.asList(payment))
            .fees(PaymentsDataUtil.getFeesData())
            .build();

        PaymentDto dto = new PaymentDtoMapper()
            .toRetrievePaymentStatusesDto(paymentFeeLink, paymentReference);

        Message msg = new Message(new ObjectMapper().writeValueAsString(dto));

        msg.setContentType("application/json");
        msg.setLabel("Service Callback Message");
        msg.setProperties(Collections.singletonMap("serviceCallbackUrl", payment.getServiceCallbackUrl()));

        client.send(msg);
        client.send(msg);
        client.send(msg);
        client.send(msg);
        client.send(msg);
        client.send(msg);
        client.send(msg);
        client.send(msg);
        client.send(msg);
        client.send(msg);
        client.send(msg);
        client.send(msg);

        client.close();

    }


}
