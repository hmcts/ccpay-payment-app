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
import jakarta.persistence.criteria.*;

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

