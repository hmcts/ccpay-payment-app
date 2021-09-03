package uk.gov.hmcts.payment.api.validators;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.DuplicatePaymentException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;


@RunWith(MockitoJUnitRunner.class)
public class DuplicatePaymentValidatorTest {

    private static final int TIME_INTERVAL = 2;

    private DuplicatePaymentValidator validator;
    @Mock
    private DuplicateSpecification duplicateSpecification;
    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Mock
    private Specification mockSpecification;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        validator = new DuplicatePaymentValidator(duplicateSpecification, TIME_INTERVAL, paymentFeeLinkRepository);
    }

    @Test
    public void shouldReturnNoErrors_whenNoMatchingPaymentsWithCriteriaSpecification() {
        Payment payment = getPayment();
        PaymentFee requestFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").volume(1).build();

        given(duplicateSpecification.getBy(payment, TIME_INTERVAL)).willReturn(mockSpecification);
        given(paymentFeeLinkRepository.findAll(mockSpecification)).willReturn(Collections.EMPTY_LIST);

        // no exception expected, none thrown: passes.
        validator.checkDuplication(payment, Lists.newArrayList(requestFee));
    }

    @Test
    public void shouldReturnNoErrors_whenMatchingPaymentsButWithDifferentFeeCode() {
        Payment payment = getPayment();
        PaymentFee requestFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").volume(1).build();
        PaymentFee dbFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0002").volume(1).build();

        PaymentFeeLink paymentFeeLink = paymentFeeLinkWith().paymentReference("RC-1519-9028-1909-3890")
            .payments(Arrays.asList(payment))
            .fees(Arrays.asList(dbFee))
            .build();
        given(duplicateSpecification.getBy(payment, TIME_INTERVAL)).willReturn(mockSpecification);
        given(paymentFeeLinkRepository.findAll(mockSpecification)).willReturn(Lists.newArrayList(paymentFeeLink));

        // no exception expected, none thrown: passes.
        validator.checkDuplication(payment, Lists.newArrayList(requestFee));
    }

    @Test
    public void shouldReturnNoErrors_whenMatchingPaymentsButWithDifferentFeeVersion() {
        Payment payment = getPayment();
        PaymentFee requestFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").volume(1).build();
        PaymentFee dbFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("4").code("X0001").volume(1).build();

        PaymentFeeLink paymentFeeLink = paymentFeeLinkWith().paymentReference("RC-1519-9028-1909-3890")
            .payments(Arrays.asList(payment))
            .fees(Arrays.asList(dbFee))
            .build();
        given(duplicateSpecification.getBy(payment, TIME_INTERVAL)).willReturn(mockSpecification);
        given(paymentFeeLinkRepository.findAll(mockSpecification)).willReturn(Lists.newArrayList(paymentFeeLink));

        // no exception expected, none thrown: passes.
        validator.checkDuplication(payment, Lists.newArrayList(requestFee));
    }

    @Test
    public void shouldReturnNoErrors_whenMatchingPaymentsButWithDifferentFeeVolume() {
        Payment payment = getPayment();
        PaymentFee requestFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").volume(1).build();
        PaymentFee dbFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").volume(4).build();

        PaymentFeeLink paymentFeeLink = paymentFeeLinkWith().paymentReference("RC-1519-9028-1909-3890")
            .payments(Arrays.asList(payment))
            .fees(Arrays.asList(dbFee))
            .build();
        given(duplicateSpecification.getBy(payment, TIME_INTERVAL)).willReturn(mockSpecification);
        given(paymentFeeLinkRepository.findAll(mockSpecification)).willReturn(Lists.newArrayList(paymentFeeLink));

        // no exception expected, none thrown: passes.
        validator.checkDuplication(payment, Lists.newArrayList(requestFee));
    }

    @Test
    public void shouldReturnNoErrors_whenMatchingPaymentsButWithMultipleFees() {
        Payment payment = getPayment();
        PaymentFee requestFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").volume(1).build();
        PaymentFee dbFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").volume(1).build();
        PaymentFee dbFee2 = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0002").volume(1).build();

        PaymentFeeLink paymentFeeLink = paymentFeeLinkWith().paymentReference("RC-1519-9028-1909-3890")
            .payments(Arrays.asList(payment))
            .fees(Arrays.asList(dbFee, dbFee2))
            .build();
        given(duplicateSpecification.getBy(payment, TIME_INTERVAL)).willReturn(mockSpecification);
        given(paymentFeeLinkRepository.findAll(mockSpecification)).willReturn(Lists.newArrayList(paymentFeeLink));

        // no exception expected, none thrown: passes.
        validator.checkDuplication(payment, Lists.newArrayList(requestFee));
    }

    @Test
    public void shouldThrowException_whenMatchingPaymentsWithSameFeeDetails() {
        Payment payment = getPayment();
        PaymentFee requestFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").volume(1).build();
        PaymentFee dbFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").volume(1).build();

        PaymentFeeLink paymentFeeLink = paymentFeeLinkWith().paymentReference("RC-1519-9028-1909-3890")
            .payments(Arrays.asList(payment))
            .fees(Arrays.asList(dbFee))
            .build();
        given(duplicateSpecification.getBy(payment, TIME_INTERVAL)).willReturn(mockSpecification);
        given(paymentFeeLinkRepository.findAll(mockSpecification)).willReturn(Lists.newArrayList(paymentFeeLink));

        //  exception expected.
        exception.expect(DuplicatePaymentException.class);
        validator.checkDuplication(payment, Lists.newArrayList(requestFee));
    }

    private Payment getPayment() {
        return Payment.paymentWith()
            .amount(new BigDecimal("11.99"))
            .ccdCaseNumber("ccdCaseNumber")
            .serviceType("Probate")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .reference("RC-1519-9028-1909-3890")
            .build();
    }

}
