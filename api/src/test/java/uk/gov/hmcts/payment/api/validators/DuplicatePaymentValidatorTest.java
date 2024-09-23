package uk.gov.hmcts.payment.api.validators;

import org.mockito.Mockito;
import org.springframework.validation.FieldError;
import uk.gov.hmcts.payment.api.contract.exception.FieldErrorDTO;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
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
import uk.gov.hmcts.payment.api.util.DateUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.DuplicatePaymentException;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.assertThrows;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.*;
import org.junit.Test;
import static org.junit.Assert.*;





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


    @InjectMocks
    private PaymentValidator paymentValidator;

    private static final Logger LOG = LoggerFactory.getLogger(PaymentValidator.class);

    DateUtil dateUtil = new DateUtil();


    @Before
    public void setUp() {
        validator = new DuplicatePaymentValidator(duplicateSpecification, TIME_INTERVAL, paymentFeeLinkRepository);
       // paymentValidator = Mockito.spy(new PaymentValidator());  // Spy allows partial mocking

        paymentValidator = Mockito.spy(new PaymentValidator(dateUtil));

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

//    @Test
//    void shouldReturnCorrectPredicateWhenCcdCaseNumberIsPresent() {
//        Payment payment = aPayment();
//        PaymentFee dbFee = requestFee;
//
//        PaymentFeeLink paymentFeeLink = paymentFeeLinkWith().paymentReference("RC-1519-9028-1909-3890")
//            .payments(Arrays.asList(payment))
//            .fees(Arrays.asList(dbFee))
//            .build();
//        given(duplicateSpecification.getBy(payment, TIME_INTERVAL)).willReturn(mockSpecification);
//        given(paymentFeeLinkRepository.findAll(mockSpecification)).willReturn(Lists.newArrayList(paymentFeeLink));
//
//
//        given(userIdSupplier.get()).willReturn("user123");
//        BDDMockito.BDDMyOngoingStubbing<Join<Object, Object>> payments = given(root.join("payments", JoinType.LEFT)).willReturn(paymentJoin);
//
////        BDDMockito.BDDMyOngoingStubbing<Path<Object>> userId = given(paymentJoin.get("userId")).willReturn(userIdPath);
////        given(paymentJoin.get("amount")).willReturn(amountPath);
////        given(paymentJoin.get("serviceType")).willReturn(serviceTypePath);
////        given(paymentJoin.get("ccdCaseNumber")).willReturn(ccdCaseNumberPath);
////        given(paymentJoin.get("dateCreated")).willReturn(dateCreatedPath);
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
//        Specification<Payment> specification = duplicateSpecification.getBy(payment, 30);
//
//        assertNotNull(specification);
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
//    }


//    @Test
//    void shouldReturnCorrectPredicateWhenCcdCaseNumberIsAbsent() {
//        Payment payment = new Payment();
//        payment.setAmount(BigDecimal.valueOf(100));
//        payment.setServiceType("serviceType1");
//        payment.setCaseReference("CASE1234");
//
//        String s = "2023-08-01T10:00:00";
//        String e = "2023-08-02T10:00:00";
//
//        Date startDatee = new Date(s);
//        Date endDatee = new Date(e);
//
//        when(userIdSupplier.get()).thenReturn("user123");
//        when(root.join("payments", JoinType.LEFT)).thenReturn(paymentJoin);
//        when(paymentJoin.get("userId")).thenReturn(userIdPath);
//        when(paymentJoin.get("amount")).thenReturn(amountPath);
//        when(paymentJoin.get("serviceType")).thenReturn(serviceTypePath);
//        when(paymentJoin.get("caseReference")).thenReturn(caseReferencePath);
//        when(paymentJoin.get("dateCreated")).thenReturn(dateCreatedPath);
//
//        when(cb.equal(userIdPath, "user123")).thenReturn(mock(Predicate.class));
//        when(cb.equal(amountPath, payment.getAmount())).thenReturn(mock(Predicate.class));
//        when(cb.equal(serviceTypePath, payment.getServiceType())).thenReturn(mock(Predicate.class));
//        when(cb.equal(caseReferencePath, payment.getCaseReference())).thenReturn(mock(Predicate.class));
//
//       // when(cb.between(eq(dateCreatedPath), any(Date.class), any(Date.class))).thenReturn(mock(Predicate.class));
//        given(cb.between(eq(dateCreatedPath), startDatee, endDatee)).thenReturn(mock(Predicate.class));
//
//        Specification<Payment> specification = duplicateSpecification.getBy(payment, 30);
//
//        assertNotNull(specification);
//
//        Predicate predicate = specification.toPredicate(root, query, cb);
//        assertNotNull(predicate);
//
//        verify(paymentJoin, times(1)).get("userId");
//        verify(paymentJoin, times(1)).get("amount");
//        verify(paymentJoin, times(1)).get("serviceType");
//        verify(paymentJoin, times(0)).get("ccdCaseNumber");
//        verify(paymentJoin, times(1)).get("caseReference");
//        verify(paymentJoin, times(1)).get("dateCreated");
//    }
    @Test
    public void shouldPassValidationWhenDatesAreValid() {
        // Arrange
        Optional<String> startDateString = Optional.of("2023-08-01T10:00:00");
        Optional<String> endDateString = Optional.of("2023-08-02T10:00:00");

        // Act & Assert
        paymentValidator.validateToFromDates(startDateString, endDateString);

        // No exception should be thrown, meaning validation was successful
        verify(paymentValidator, times(1)).validateToFromDates(startDateString, endDateString);
        LOG.info("Validation successful for valid dates.");
    }

    @Test
    public void shouldThrowErrorWhenStartDateIsAfterEndDate() {
        // Arrange
        Optional<String> startDateString = Optional.of("2023-09-03T10:00:00");
        Optional<String> endDateString = Optional.of("2023-09-02T10:00:00");

        // Act & Assert
        ValidationErrorException exception = assertThrows(ValidationErrorException.class, () -> {
            paymentValidator.validateToFromDates(startDateString, endDateString);
        });


    }

    @Test
    public void shouldThrowErrorWhenStartDateIsInvalid() {
        // Arrange
        Optional<String> startDateString = Optional.of("2024-09-02T10:00:00");
        Optional<String> endDateString = Optional.of("2023-09-02T10:00:00");

        // Act & Assert
//        ValidationErrorException exception = assertThrows(ValidationErrorException.class, () -> {
//            paymentValidator.validateToFromDates(startDateString, endDateString);
//        });
//
//        // Assert that validation errors were added for invalid start date
//      //  ValidationErrorDTO dto = exception.getValidationErrorDTO();
//        ValidationErrorDTO dto = exception.getErrors();
//
//
//      //  assertTrue(dto.hasFieldErrors());
//        verify(dto.hasErrors());
//
//        verify(dto.getFieldErrors().stream().anyMatch(error ->
//            "start_date".equals(error.getField()) && error.getMessage().contains("Invalid date format")
       // ));
        //Arguments capture potetial solution use spy check to see
        // Set up validationErrorDto as spy obect check to see if hasErrors() ever gets called?
        //set up verify test for spy obect
        //Set up test for everi field ln60 addFieldError


        //expected exception
        //assert nothing because method has no output
        // if no exception then it has passed
        //call in test with incorrect values and expect an exception
        //then open expetion and we try to retrive dto if possible
        // do this by:


        // :Validation failed
        //Actual   :Error occurred in the payment params

        try {
            // Act
            paymentValidator.validateToFromDates(startDateString, endDateString);
            fail("Expected ValidationErrorException to be thrown");  // This ensures the test fails if no exception is thrown
        } catch (ValidationErrorException ex) {
            // Assert: Check the exception message
            assertEquals("Error occurred in the payment params", ex.getMessage());

            // Assert: Check the validation errors in ValidationErrorDTO
            ValidationErrorDTO dto = ex.getErrors();
            assertNotNull(dto);
            assertFalse(dto.getFieldErrors().isEmpty());

            // Assert: Check specific error field and message
            FieldErrorDTO fieldError = dto.getFieldErrors().get(0);
            assertEquals("dates", fieldError.getField());
            assertEquals("Start date cannot be greater than end date", fieldError.getMessage());
        }
    }


    @Test
    public void shouldThrowErrorWhenEndDateIsInvalid() {
        // Arrange
        Optional<String> startDateString = Optional.of("2023-08-01T10:00:00");
        Optional<String> endDateString = Optional.of("invalid-date");

        // Act & Assert
        ValidationErrorException exception = assertThrows(ValidationErrorException.class, () -> {
            paymentValidator.validateToFromDates(startDateString, endDateString);
        });

        // Assert that validation errors were added for invalid end date
        ValidationErrorDTO dto = exception.getErrors();
       assertTrue(dto.hasErrors());
        assertTrue(dto.getFieldErrors().stream().anyMatch(error ->
            "end_date".equals(error.getField()) && error.getMessage().contains("Invalid date format")
        ));
    }

    @Test
    public void shouldThrowErrorWhenBothDatesAreInvalid() {
        // Arrange
        Optional<String> startDateString = Optional.of("invalid-date");
        Optional<String> endDateString = Optional.of("another-invalid-date");

        // Act & Assert
        ValidationErrorException exception = assertThrows(ValidationErrorException.class, () -> {
            paymentValidator.validateToFromDates(startDateString, endDateString);
        });

        // Assert that validation errors were added for both start and end dates
        ValidationErrorDTO dto = exception.getErrors();
        assertTrue(dto.hasErrors());
        assertTrue(dto.getFieldErrors().stream().anyMatch(error ->
           "start_date".equals(error.getField()) && error.getMessage().contains("Invalid date format")
        ));
        assertTrue(dto.getFieldErrors().stream().anyMatch(error ->
            "end_date".equals(error.getField()) && error.getMessage().contains("Invalid date format")
        ));

    }

    @Test
    public void shouldPassWhenBothDatesAreEmpty() {
        // Arrange
        Optional<String> startDateString = Optional.empty();
        Optional<String> endDateString = Optional.empty();

        // Act & Assert
        paymentValidator.validateToFromDates(startDateString, endDateString);

        // No exception should be thrown, meaning validation was successful
        verify(paymentValidator, times(1)).validateToFromDates(startDateString, endDateString);
        LOG.info("Validation successful for empty dates.");
    }
}

