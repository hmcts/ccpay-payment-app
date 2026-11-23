package uk.gov.hmcts.payment.api.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
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
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.CallbackService;
import uk.gov.hmcts.payment.api.servicebus.CallbackServiceImpl;
import uk.gov.hmcts.payment.api.servicebus.TopicClientProxy;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@RunWith(MockitoJUnitRunner.class)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class CallbackServiceImplTest {

    PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
        .payments(Arrays.asList(CardPaymentComponentTest.getPaymentsData().get(2)))
        .fees(PaymentsDataUtil.getFeesData())
        .build();
    @Mock
    private TopicClientProxy topicClient;
    private CallbackService callbackService;
    @Mock
    private PaymentDtoMapper paymentDtoMapper;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentGroupDtoMapper paymentGroupDtoMapper;

    @Before
    public void init() {
        callbackService = new CallbackServiceImpl(paymentDtoMapper, objectMapper, topicClient, paymentGroupDtoMapper);
    }

    @After
    public void tearDown() {
        callbackService = null;
        Thread.interrupted(); // clear any interrupt flag set during the test
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

        verifyNoInteractions(topicClient);

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
    public void testThatWhenThreadInterruptedBusIsNotCalled() {
        try {
            Thread.currentThread().interrupt();

            paymentFeeLink.getPayments().get(0).setServiceCallbackUrl(null);
            paymentFeeLink.setCallBackUrl("dummy");

            when(paymentGroupDtoMapper.toPaymentGroupDto(any()))
                .thenReturn(new PaymentGroupDto());

            callbackService.callback(
                paymentFeeLink,
                paymentFeeLink.getPayments().get(0)
            );

            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            // Ensure this test never leaks its deliberately-set interrupt state.
            Thread.interrupted();
        }
    }

    @Test
    public void testThatServiceCallbackSendFailureDoesNotInterruptThread() throws Exception {
        Thread.interrupted(); // clear any pre-existing interrupt flag
        doThrow(new RuntimeException("send failed"))
            .when(topicClient).send(any(IMessage.class));

        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));

        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    public void testThatServiceCallbackSendInterruptedExceptionInterruptsThread() throws Exception {
        Thread.interrupted(); // clear any pre-existing interrupt flag
        doThrow(new InterruptedException())
            .when(topicClient).send(any(IMessage.class));

        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));

        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    public void testThatPaymentFeeLinkCallbackSendFailureDoesNotInterruptThread() throws Exception {
        Thread.interrupted(); // clear any pre-existing interrupt flag
        paymentFeeLink.getPayments().get(0).setServiceCallbackUrl(null);
        paymentFeeLink.setCallBackUrl("dummy");

        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(new PaymentGroupDto());
        doThrow(new RuntimeException("send failed"))
            .when(topicClient).send(any(IMessage.class));

        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));

        assertFalse(Thread.currentThread().isInterrupted());
    }

@Test
    public void testThatWhenPaymentFeeLinkCallbackSendInterruptedExceptionInterruptsThread() throws Exception {
        Thread.interrupted(); // clear any pre-existing interrupt flag
        paymentFeeLink.getPayments().get(0).setServiceCallbackUrl(null);
        paymentFeeLink.setCallBackUrl("dummy");

        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(new PaymentGroupDto());
        doThrow(new InterruptedException())
            .when(topicClient).send(any(IMessage.class));

        callbackService.callback(paymentFeeLink, paymentFeeLink.getPayments().get(0));

        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    public void testThatServiceRequestCallbackRedirectsToSuccessfulPaymentWhenAlreadyPaid() throws Exception {
        Payment failedPayment = CardPaymentComponentTest.getPaymentsData().get(2);
        failedPayment.setServiceCallbackUrl(null);
        failedPayment.setPaymentStatus(PaymentStatus.FAILED);
        Payment successfulPayment = CardPaymentComponentTest.getPaymentsData().get(0);
        successfulPayment.setServiceCallbackUrl(null);
        successfulPayment.setPaymentStatus(PaymentStatus.SUCCESS);
        PaymentFeeLink feeLink = buildPaymentFeeLink(failedPayment, successfulPayment);

        PaymentGroupDto paymentGroupDto = new PaymentGroupDto();
        paymentGroupDto.setServiceRequestStatus("Paid");
        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(paymentGroupDto);

        callbackService.callback(feeLink, failedPayment);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentDtoMapper).toPaymentStatusDto(eq("00000005"), eq(""), paymentCaptor.capture(), any());
        assertEquals(successfulPayment.getReference(), paymentCaptor.getValue().getReference());
        verify(topicClient, times(1)).send(any(IMessage.class));
    }

    @Test
    public void testThatCancelledPaymentServiceRequestCallbackRedirectsToSuccessfulPaymentWhenAlreadyPaid() throws Exception {
        Payment cancelledPayment = CardPaymentComponentTest.getPaymentsData().get(2);
        cancelledPayment.setServiceCallbackUrl(null);
        cancelledPayment.setPaymentStatus(PaymentStatus.CANCELLED);
        Payment successfulPayment = CardPaymentComponentTest.getPaymentsData().get(0);
        successfulPayment.setServiceCallbackUrl(null);
        successfulPayment.setPaymentStatus(PaymentStatus.SUCCESS);
        PaymentFeeLink feeLink = buildPaymentFeeLink(cancelledPayment, successfulPayment);

        PaymentGroupDto paymentGroupDto = new PaymentGroupDto();
        paymentGroupDto.setServiceRequestStatus("Paid");
        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(paymentGroupDto);

        callbackService.callback(feeLink, cancelledPayment);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentDtoMapper).toPaymentStatusDto(eq("00000005"), eq(""), paymentCaptor.capture(), any());
        assertEquals(successfulPayment.getReference(), paymentCaptor.getValue().getReference());
        verify(topicClient, times(1)).send(any(IMessage.class));
    }

    @Test
    public void testThatFailedPaymentCallbackIsSentWhenServiceRequestIsNotPaid() throws Exception {
        Payment failedPayment = CardPaymentComponentTest.getPaymentsData().get(2);
        failedPayment.setServiceCallbackUrl(null);
        failedPayment.setPaymentStatus(PaymentStatus.FAILED);
        Payment successfulPayment = CardPaymentComponentTest.getPaymentsData().get(0);
        successfulPayment.setServiceCallbackUrl(null);
        successfulPayment.setPaymentStatus(PaymentStatus.SUCCESS);
        PaymentFeeLink feeLink = buildPaymentFeeLink(failedPayment, successfulPayment);

        PaymentGroupDto paymentGroupDto = new PaymentGroupDto();
        paymentGroupDto.setServiceRequestStatus("Not paid");
        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(paymentGroupDto);

        callbackService.callback(feeLink, failedPayment);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentDtoMapper).toPaymentStatusDto(eq("00000005"), eq(""), paymentCaptor.capture(), any());
        assertEquals(failedPayment.getReference(), paymentCaptor.getValue().getReference());
        verify(topicClient, times(1)).send(any(IMessage.class));
    }

    @Test
    public void testThatServiceRequestCallbackFallsBackToOriginalPaymentWhenNoSuccessPaymentExists() throws Exception {
        Payment failedPayment = CardPaymentComponentTest.getPaymentsData().get(2);
        failedPayment.setServiceCallbackUrl(null);
        failedPayment.setPaymentStatus(PaymentStatus.FAILED);
        PaymentFeeLink feeLink = buildPaymentFeeLink(failedPayment);

        PaymentGroupDto paymentGroupDto = new PaymentGroupDto();
        paymentGroupDto.setServiceRequestStatus("Paid");
        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(paymentGroupDto);

        callbackService.callback(feeLink, failedPayment);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentDtoMapper).toPaymentStatusDto(eq("00000005"), eq(""), paymentCaptor.capture(), any());
        assertEquals(failedPayment.getReference(), paymentCaptor.getValue().getReference());
        verify(topicClient, times(1)).send(any(IMessage.class));
    }

    @Test
    public void testThatSuccessfulPaymentCallbackIsSentWhenServiceRequestIsPaid() throws Exception {
        Payment successfulPayment = CardPaymentComponentTest.getPaymentsData().get(0);
        successfulPayment.setServiceCallbackUrl(null);
        successfulPayment.setPaymentStatus(PaymentStatus.SUCCESS);
        PaymentFeeLink feeLink = buildPaymentFeeLink(successfulPayment);

        PaymentGroupDto paymentGroupDto = new PaymentGroupDto();
        paymentGroupDto.setServiceRequestStatus("Paid");
        when(paymentGroupDtoMapper.toPaymentGroupDto(any())).thenReturn(paymentGroupDto);

        callbackService.callback(feeLink, successfulPayment);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentDtoMapper).toPaymentStatusDto(eq("00000005"), eq(""), paymentCaptor.capture(), any());
        assertEquals(successfulPayment.getReference(), paymentCaptor.getValue().getReference());
        verify(topicClient, times(1)).send(any(IMessage.class));
    }

    private PaymentFeeLink buildPaymentFeeLink(Payment... payments) {
        return PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
            .payments(Arrays.asList(payments))
            .fees(PaymentsDataUtil.getFeesData())
            .callBackUrl("dummy")
            .build();
    }
}
