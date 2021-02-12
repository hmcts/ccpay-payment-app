package uk.gov.hmcts.payment.api.servicebus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import org.ff4j.FF4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.payment.api.componenttests.CardPaymentComponentTest;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.CallbackService;
import uk.gov.hmcts.payment.api.servicebus.CallbackServiceImpl;
import uk.gov.hmcts.payment.api.servicebus.TopicClientProxy;

import java.util.Arrays;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LoggerFactory.class})
public class CallbackServiceImplTest {

    private TopicClientProxy topicClient;

    private FF4j ff4j = new FF4j();

    private CallbackService callbackService;

    private PaymentDtoMapper paymentDtoMapper = new PaymentDtoMapper();

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void init() {
        callbackService = new CallbackServiceImpl(paymentDtoMapper, objectMapper, topicClient, ff4j);
        when(ff4j.check(CallbackService.FEATURE)).thenReturn(true);
    }

    @Test
    public void testThatWhenCallbackUriIsProvidedServiceBusIsCalled() throws Exception {

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
            .payments(Arrays.asList(CardPaymentComponentTest.getPaymentsData().get(2)))
            .fees(PaymentsDataUtil.getFeesData())
            .build();

        when(paymentDtoMapper.toResponseDto(paymentFeeLink, paymentFeeLink.getPayments().get(0))).thenReturn(new PaymentDto());

        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));

        verify((topicClient), times(1)).send(any(IMessage.class));

    }

    @Test
    public void testThatWhenNoCallbackUrlIsProvidedBusIsNotCalled() {

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
            .payments(Arrays.asList(CardPaymentComponentTest.getPaymentsData().get(2)))
            .fees(PaymentsDataUtil.getFeesData())
            .build();
        paymentFeeLink.getPayments().get(0).setServiceCallbackUrl(null);
        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));
        verifyZeroInteractions(topicClient);

    }


}
