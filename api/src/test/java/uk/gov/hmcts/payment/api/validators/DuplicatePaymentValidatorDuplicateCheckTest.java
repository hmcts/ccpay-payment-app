package uk.gov.hmcts.payment.api.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.v1.model.exceptions.DuplicatePaymentException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

class DuplicatePaymentValidatorDuplicateCheckTest {

    private static final int TIME_INTERVAL = 2;
    private static final PaymentFee REQUEST_FEE = PaymentFee.feeWith()
        .calculatedAmount(new BigDecimal("11.99"))
        .version("1")
        .code("X0001")
        .volume(1)
        .build();

    private DuplicateSpecification duplicateSpecification;
    private PaymentFeeLinkRepository paymentFeeLinkRepository;
    private Specification<PaymentFeeLink> specification;
    private DuplicatePaymentValidator validator;

    @BeforeEach
    void setUp() {
        duplicateSpecification = mock(DuplicateSpecification.class);
        paymentFeeLinkRepository = mock(PaymentFeeLinkRepository.class);
        specification = mock(Specification.class);
        validator = new DuplicatePaymentValidator(duplicateSpecification, TIME_INTERVAL, paymentFeeLinkRepository);
    }

    @Test
    void throwsWhenFeesMatchAndExistingPaymentSucceeded() {
        Payment requestPayment = paymentWithStatus("pending");
        Payment existingPayment = paymentWithStatus(PaymentStatus.SUCCESS.getName());
        PaymentFeeLink existingLink = paymentFeeLinkWith()
            .payments(List.of(existingPayment))
            .fees(List.of(REQUEST_FEE))
            .build();
        givenExistingPayments(requestPayment, List.of(existingLink));

        assertThatThrownBy(() -> validator.checkDuplication(requestPayment, List.of(REQUEST_FEE)))
            .isInstanceOf(DuplicatePaymentException.class)
            .hasMessage("duplicate payment");
    }

    @Test
    void doesNotThrowWhenFeesMatchButExistingPaymentDidNotSucceed() {
        Payment requestPayment = paymentWithStatus("pending");
        Payment existingPayment = paymentWithStatus("failed");
        PaymentFeeLink existingLink = paymentFeeLinkWith()
            .payments(List.of(existingPayment))
            .fees(List.of(REQUEST_FEE))
            .build();
        givenExistingPayments(requestPayment, List.of(existingLink));

        validator.checkDuplication(requestPayment, List.of(REQUEST_FEE));
    }

    @Test
    void successCheckReturnsFalseForNullOrEmptyResults() {
        assertThat(hasSuccessfulPayment(null)).isFalse();
        assertThat(hasSuccessfulPayment(List.of())).isFalse();
    }

    @Test
    void successCheckHandlesNullPaymentsAndNullPaymentStatus() {
        PaymentFeeLink linkWithoutPayments = paymentFeeLinkWith().build();
        PaymentFeeLink linkWithPaymentWithoutStatus = paymentFeeLinkWith()
            .payments(List.of(Payment.paymentWith().build()))
            .build();

        assertThat(hasSuccessfulPayment(List.of(linkWithoutPayments))).isFalse();
        assertThat(hasSuccessfulPayment(List.of(linkWithPaymentWithoutStatus))).isFalse();
    }

    @Test
    void successCheckReturnsTrueForSuccessfulPayment() {
        PaymentFeeLink link = paymentFeeLinkWith()
            .payments(List.of(paymentWithStatus(PaymentStatus.SUCCESS.getName())))
            .build();

        assertThat(hasSuccessfulPayment(List.of(link))).isTrue();
    }

    private void givenExistingPayments(Payment payment, List<PaymentFeeLink> results) {
        when(duplicateSpecification.getBy(payment, TIME_INTERVAL)).thenReturn(specification);
        when(paymentFeeLinkRepository.findAll(specification)).thenReturn(results);
    }

    private boolean hasSuccessfulPayment(List<PaymentFeeLink> paymentFeeLinks) {
        return ReflectionTestUtils.invokeMethod(validator, "isThereAnySuccessfullyPayment", (Object) paymentFeeLinks);
    }

    private Payment paymentWithStatus(String status) {
        return Payment.paymentWith()
            .ccdCaseNumber("case-number")
            .paymentStatus(PaymentStatus.paymentStatusWith().name(status).build())
            .build();
    }
}
