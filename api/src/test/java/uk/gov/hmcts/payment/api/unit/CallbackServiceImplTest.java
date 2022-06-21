package uk.gov.hmcts.payment.api.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.ff4j.FF4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.payment.api.componenttests.CardPaymentComponentTest;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.CallbackService;
import uk.gov.hmcts.payment.api.servicebus.CallbackServiceImpl;
import uk.gov.hmcts.payment.api.servicebus.TopicClientProxy;

import java.util.Arrays;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class CallbackServiceImplTest {

    PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
        .payments(Arrays.asList(CardPaymentComponentTest.getPaymentsData().get(2)))
        .fees(PaymentsDataUtil.getFeesData())
        .build();
    @Mock
    private TopicClientProxy topicClient;
    @Mock
    private FF4j ff4j;
    private CallbackService callbackService;
    @Mock
    private PaymentDtoMapper paymentDtoMapper;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentGroupDtoMapper paymentGroupDtoMapper;

    @Before
    public void init() {
        callbackService = new CallbackServiceImpl(paymentDtoMapper, objectMapper, topicClient, ff4j,
                paymentGroupDtoMapper);
        when(ff4j.check(CallbackService.FEATURE)).thenReturn(true);
    }

    @After
    public void tearDown() {
        callbackService = null;
    }

    @Test
    public void testThatWhenCallbackUriIsProvidedServiceBusIsCalled() throws Exception {

        when(paymentDtoMapper.toResponseDto(paymentFeeLink, paymentFeeLink.getPayments().get(0))).thenReturn(new PaymentDto());

        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));

        verify((topicClient), times(1)).send(any(IMessage.class));

    }

    @Test
    public void testThatWhenNoCallbackUrlIsProvidedBusIsNotCalled() {

        paymentFeeLink.getPayments().get(0).setServiceCallbackUrl(null);

        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));

        verifyZeroInteractions(topicClient);

    }

    @Test
    public void testThatWhenCallbackUrlIsProvidedInPaymentFeeLinkBusIsCalled() throws ServiceBusException, InterruptedException {

        paymentFeeLink.getPayments().get(0).setServiceCallbackUrl(null);
        paymentFeeLink.setCallBackUrl("dummy");

        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(new PaymentGroupDto());

        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));

        verify((topicClient), times(1)).send(any(IMessage.class));
    }

    @Test
    public void testThatWhenFeatureIsOffInPaymentFeeLinkBusIsNotCalled() throws ServiceBusException, InterruptedException {

        when(ff4j.check(CallbackService.FEATURE)).thenReturn(false);

        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));

        verifyZeroInteractions(topicClient);
    }
}
