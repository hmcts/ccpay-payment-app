package uk.gov.hmcts.payment.api.servicebus;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.componenttests.CardPaymentComponentTest;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.configuration.FeatureFlags;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.CallbackService;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class CallbackServiceImplTest {

    @Autowired
    private CallbackServiceImpl callbackService;

    @MockBean
    private TopicClientProxy topicClient;

    @MockBean
    private FeatureFlags featureFlagsMock;

    @Test
    public void testCallbackService() throws ServiceBusException, InterruptedException {

        Payment payment = CardPaymentComponentTest.getPaymentsData().get(2);
        payment.setServiceCallbackUrl("serviceCallbackUrl");
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
            .payments(Arrays.asList(payment))
            .fees(PaymentsDataUtil.getFeesData())
            .build();
        doNothing().when(topicClient).send(any(IMessage.class));
        when(featureFlagsMock.check("payment_service_callback")).thenReturn(true);
        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));
        Mockito.verify(topicClient).send(any());
    }
}
