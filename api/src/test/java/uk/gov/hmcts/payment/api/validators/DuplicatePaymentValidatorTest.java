package uk.gov.hmcts.payment.api.validators;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.DuplicatePaymentException;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentChannel;


import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import javax.persistence.criteria.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import java.util.Date;
import static org.mockito.ArgumentMatchers.eq;



@RunWith(MockitoJUnitRunner.class)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class DuplicatePaymentValidatorTest {

    private final static int TIME_INTERVAL = 2;
    static PaymentFee requestFee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").volume(1).build();
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    @Mock
    private DuplicatePaymentValidator validator;
    @Mock
    private DuplicateSpecification duplicateSpecification;
    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;
    @Mock
    private Specification mockSpecification;
    @Mock
    private UserIdSupplier userIdSupplier;
    @Mock
    private Join<Object, Object> paymentJoin;


    @Mock
    private Root<Payment> root;

    @Mock
    private CriteriaBuilder cb;



    @Mock
    private Path<String> userIdPath;

    @Mock
    private Path<BigDecimal> amountPath;

    @Mock
    private Path<String> serviceTypePath;

    @Mock
    private Path<String> ccdCaseNumberPath;

    @Mock
    private Path<String> caseReferencePath;

    @Mock
    private Path<PaymentChannel> paymentChannelPath;

    @Mock
    private Path<Date> dateCreatedPath;

//    @InjectMocks
//    private DuplicateSpecification duplicateSpecification;





    @Before
    public void setUp() {
        validator = new DuplicatePaymentValidator(duplicateSpecification, TIME_INTERVAL, paymentFeeLinkRepository);



    }

    @After
    public void tearDown() {
        validator = null;
    }

    @Test
    public void shouldReturnNoErrors_whenNoMatchingPaymentsWithCriteriaSpecification() {
        Payment payment = aPayment();


        given(duplicateSpecification.getBy(payment, TIME_INTERVAL)).willReturn(mockSpecification);
        given(paymentFeeLinkRepository.findAll(mockSpecification)).willReturn(Collections.EMPTY_LIST);

        // no exception expected, none thrown: passes.
        validator.checkDuplication(payment, Lists.newArrayList(requestFee));
    }

    @Test
    public void shouldReturnNoErrors_whenMatchingPaymentsButWithDifferentFeeCode() {
        Payment payment = aPayment();
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
        Payment payment = aPayment();
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
        Payment payment = aPayment();
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
        Payment payment = aPayment();
        PaymentFee dbFee = requestFee;
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
        Payment payment = aPayment();
        PaymentFee dbFee = requestFee;

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

    private Payment aPayment() {
        return Payment.paymentWith()
            .amount(new BigDecimal("11.99"))
            .ccdCaseNumber("ccdCaseNumber")
            .serviceType("Probate")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .reference("RC-1519-9028-1909-3890")
            .build();
    }

    @Test
    void shouldReturnCorrectPredicateWhenCcdCaseNumberIsPresent() {
        Payment payment = aPayment();
        PaymentFee dbFee = requestFee;

        PaymentFeeLink paymentFeeLink = paymentFeeLinkWith().paymentReference("RC-1519-9028-1909-3890")
            .payments(Arrays.asList(payment))
            .fees(Arrays.asList(dbFee))
            .build();
        given(duplicateSpecification.getBy(payment, TIME_INTERVAL)).willReturn(mockSpecification);
        given(paymentFeeLinkRepository.findAll(mockSpecification)).willReturn(Lists.newArrayList(paymentFeeLink));





        given(userIdSupplier.get()).willReturn("user123");
        BDDMockito.BDDMyOngoingStubbing<Join<Object, Object>> payments = given(root.join("payments", JoinType.LEFT)).willReturn(paymentJoin);
//
//        BDDMockito.BDDMyOngoingStubbing<Path<Object>> userId = given(paymentJoin.get("userId")).willReturn(userIdPath);
//        given(paymentJoin.get("amount")).willReturn(amountPath);
//        given(paymentJoin.get("serviceType")).willReturn(serviceTypePath);
//        given(paymentJoin.get("ccdCaseNumber")).willReturn(ccdCaseNumberPath);
//        given(paymentJoin.get("dateCreated")).willReturn(dateCreatedPath);
//
//        given(cb.equal(userIdPath, "user123")).willReturn(mock(Predicate.class));
//        given(cb.equal(amountPath, payment.getAmount())).willReturn(mock(Predicate.class));
//        given(cb.equal(serviceTypePath, payment.getServiceType())).willReturn(mock(Predicate.class));
//        given(cb.equal(ccdCaseNumberPath, payment.getCcdCaseNumber())).willReturn(mock(Predicate.class));
//        given(cb.between(eq(dateCreatedPath), any(Date.class), any(Date.class))).willReturn(mock(Predicate.class));
//
//        given(duplicateSpecification.getBy(payment, TIME_INTERVAL)).willReturn(mockSpecification);
//        given(paymentFeeLinkRepository.findAll(mockSpecification)).willReturn(Lists.newArrayList(paymentFeeLink));
//
//
//
//
//        Predicate predicate = specification.toPredicate(root, query, cb);
//        assertNotNull(predicate);
//
//        verify(paymentJoin, times(1)).get("userId");
//        verify(paymentJoin, times(1)).get("amount");
//        verify(paymentJoin, times(1)).get("serviceType");
//        verify(paymentJoin, times(1)).get("ccdCaseNumber");
//        verify(paymentJoin, times(0)).get("caseReference");
//        verify(paymentJoin, times(1)).get("dateCreated");
    }

    @Test
    void shouldReturnCorrectPredicateWhenCcdCaseNumberIsAbsent() {
        Payment payment = aPayment();
        PaymentFee dbFee = requestFee;

        PaymentFeeLink paymentFeeLink = paymentFeeLinkWith().paymentReference("RC-1519-9028-1909-3890")
            .payments(Arrays.asList(payment))
            .fees(Arrays.asList(dbFee))
            .build();
        given(duplicateSpecification.getBy(payment, TIME_INTERVAL)).willReturn(mockSpecification);
        given(paymentFeeLinkRepository.findAll(mockSpecification)).willReturn(Lists.newArrayList(paymentFeeLink));
    }



}
