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

        TopicClientProxy client = new TopicClientProxy("Endpoint=sb://ccpay-servicebus-aat.servicebus.windows.net/;SharedAccessKeyName=SendAndListenSharedAccessKey;SharedAccessKey=ATfNGbCzML84hjtH8T0EoAyI1PyLvxMQ/VOd1lr6UCA=", "serviceCallbackTopic");

        Payment payment = CardPaymentComponentTest.getPaymentsData().get(2);

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
            .payments(Arrays.asList(payment))
            .fees(PaymentsDataUtil.getFeesData())
            .build();

        PaymentDto dto = new PaymentDtoMapper()
            .toRetrievePaymentStatusesDto(paymentFeeLink);

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
