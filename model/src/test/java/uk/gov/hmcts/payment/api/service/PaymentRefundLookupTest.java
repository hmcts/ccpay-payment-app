package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRefundLookupTest {

    private static final String FIRST_PAYMENT_REFERENCE = "payment_reference_1";
    private static final String SECOND_PAYMENT_REFERENCE = "payment_reference_2";

    @Mock
    private Payment2Repository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldReturnBothRequestedPaymentsOnSameServiceRequest(boolean reverseServiceRequestOrder) {
        List<Payment> payments = paymentsOnSameServiceRequest(reverseServiceRequestOrder);
        List<String> references = List.of(FIRST_PAYMENT_REFERENCE, SECOND_PAYMENT_REFERENCE);
        when(paymentRepository.findByReferenceIn(references)).thenReturn(payments);

        List<Payment> result = paymentService.retrievePayment(references);

        assertThat(result).extracting(Payment::getReference)
            .containsExactlyInAnyOrder(FIRST_PAYMENT_REFERENCE, SECOND_PAYMENT_REFERENCE);
    }

    @Test
    void shouldReturnOnlyRequestedPaymentWhenServiceRequestHasOtherPayments() {
        List<Payment> payments = paymentsOnSameServiceRequest(false);
        List<String> references = List.of(SECOND_PAYMENT_REFERENCE);
        when(paymentRepository.findByReferenceIn(references)).thenReturn(List.of(payments.get(1)));

        List<Payment> result = paymentService.retrievePayment(references);

        assertThat(result).extracting(Payment::getReference).containsExactly(SECOND_PAYMENT_REFERENCE);
    }

    @Test
    void shouldReturnRequestedPaymentsFromDifferentServiceRequests() {
        List<Payment> payments = paymentsOnSameServiceRequest(false);
        Payment firstPayment = payments.get(0);
        Payment secondPayment = payments.get(1);
        firstPayment.getPaymentLink().setPayments(List.of(firstPayment));
        PaymentFeeLink secondServiceRequest = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("sr_reference")
            .payments(List.of(secondPayment))
            .build();
        secondPayment.setPaymentLink(secondServiceRequest);
        List<String> references = List.of(FIRST_PAYMENT_REFERENCE, SECOND_PAYMENT_REFERENCE);
        when(paymentRepository.findByReferenceIn(references)).thenReturn(payments);

        List<Payment> result = paymentService.retrievePayment(references);

        assertThat(result).extracting(Payment::getReference)
            .containsExactlyInAnyOrder(FIRST_PAYMENT_REFERENCE, SECOND_PAYMENT_REFERENCE);
    }

    private List<Payment> paymentsOnSameServiceRequest(boolean reverseOrder) {
        PaymentFeeLink serviceRequest = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("sr_reference")
            .build();
        Payment firstPayment = Payment.paymentWith()
            .reference(FIRST_PAYMENT_REFERENCE)
            .paymentLink(serviceRequest)
            .build();
        Payment secondPayment = Payment.paymentWith()
            .reference(SECOND_PAYMENT_REFERENCE)
            .paymentLink(serviceRequest)
            .build();
        serviceRequest.setPayments(reverseOrder
            ? List.of(secondPayment, firstPayment) : List.of(firstPayment, secondPayment));
        return List.of(firstPayment, secondPayment);
    }
}
