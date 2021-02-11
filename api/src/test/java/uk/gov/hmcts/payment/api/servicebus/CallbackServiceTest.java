package uk.gov.hmcts.payment.api.servicebus;


import org.ff4j.FF4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.componenttests.CardPaymentComponentTest;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.CallbackService;
import uk.gov.hmcts.payment.api.servicebus.CallbackServiceImpl;

import java.util.Arrays;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class CallbackServiceTest {

    @Value("${service.callback.url}")
    private String serviceCallbackUrl;

    @Autowired
    private CallbackServiceImpl callbackService;

    @Autowired
    private FF4j ff4j;

    @Test
    public void testSuitesRequireAtLeastOneTest() {

    }

    @Test
    //Enable and config on application-componenttest.properties for end to end testing
    public void testCallbackService() {

        Payment payment = CardPaymentComponentTest.getPaymentsData().get(2);
        payment.setServiceCallbackUrl(serviceCallbackUrl);

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
            .payments(Arrays.asList(payment))
            .fees(PaymentsDataUtil.getFeesData())
            .build();

        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));

        ff4j.disable(CallbackService.FEATURE);

    }

    @Before
    public void init() {
        ff4j.enable(CallbackService.FEATURE);
    }

    @After
    public void clean() {
        ff4j.disable(CallbackService.FEATURE);
    }

}
