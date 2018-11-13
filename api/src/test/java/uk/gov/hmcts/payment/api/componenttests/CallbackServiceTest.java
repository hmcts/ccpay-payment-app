package uk.gov.hmcts.payment.api.componenttests;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.servicebus.CallbackService;

import java.util.Arrays;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class CallbackServiceTest {

    @Autowired
    private CallbackService callbackService;

    @Test
    public void testCallbackService() {
        callbackService.callback(PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
            .payments(Arrays.asList(CardPaymentComponentTest.getPaymentsData().get(2)))
            .fees(PaymentsDataUtil.getFeesData())
            .build());

        callbackService.callbackAmpq(PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
            .payments(Arrays.asList(CardPaymentComponentTest.getPaymentsData().get(2)))
            .fees(PaymentsDataUtil.getFeesData())
            .build());

    }

}
