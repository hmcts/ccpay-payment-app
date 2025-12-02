package uk.gov.hmcts.payment.api.contract;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TelephonyCardPaymentsRequestTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeClass
    public static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterClass
    public static void close() {
        validatorFactory.close();
    }

    @Test
    public void testValidRequest() {
        TelephonyCardPaymentsRequest request = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(BigDecimal.valueOf(100.00))
            .ccdCaseNumber("1234567890123456")
            .caseType("case-type")
            .currency(CurrencyCode.GBP)
            .returnURL("http://example.com")
            .telephonySystem("KERV")
            .build();

        Set<ConstraintViolation<TelephonyCardPaymentsRequest>> violations = validator.validate(request);
        assertEquals(0, violations.size());
    }

    @Test
    public void testInvalidAmount() {
        TelephonyCardPaymentsRequest request = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(BigDecimal.valueOf(-1))
            .ccdCaseNumber("1234567890123456")
            .caseType("case-type")
            .currency(CurrencyCode.GBP)
            .returnURL("http://example.com")
            .telephonySystem("KERV")
            .build();

        Set<ConstraintViolation<TelephonyCardPaymentsRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("must be greater than"));
    }

    @Test
    public void testInvalidCcdCaseNumber() {
        TelephonyCardPaymentsRequest request = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(BigDecimal.valueOf(100.00))
            .ccdCaseNumber("123")
            .caseType("case-type")
            .currency(CurrencyCode.GBP)
            .returnURL("http://example.com")
            .telephonySystem("KERV")
            .build();

        Set<ConstraintViolation<TelephonyCardPaymentsRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("ccd_case_number length must be 16 digits", violations.iterator().next().getMessage());
    }

    @Test
    public void testInvalidCaseType() {
        TelephonyCardPaymentsRequest request = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(BigDecimal.valueOf(100.00))
            .ccdCaseNumber("1234567890123456")
            .caseType("")
            .currency(CurrencyCode.GBP)
            .returnURL("http://example.com")
            .telephonySystem("KERV")
            .build();

        Set<ConstraintViolation<TelephonyCardPaymentsRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("must not be blank", violations.iterator().next().getMessage());
    }

    @Test
    public void testInvalidCurrency() {
        TelephonyCardPaymentsRequest request = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(BigDecimal.valueOf(100.00))
            .ccdCaseNumber("1234567890123456")
            .caseType("case-type")
            .currency(null)
            .returnURL("http://example.com")
            .telephonySystem("KERV")
            .build();

        Set<ConstraintViolation<TelephonyCardPaymentsRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("must not be null", violations.iterator().next().getMessage());
    }

    @Test
    public void testInvalidReturnURL() {
        TelephonyCardPaymentsRequest request = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(BigDecimal.valueOf(100.00))
            .ccdCaseNumber("1234567890123456")
            .caseType("case-type")
            .currency(CurrencyCode.GBP)
            .returnURL("")
            .telephonySystem("KERV")
            .build();

        Set<ConstraintViolation<TelephonyCardPaymentsRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("must not be empty", violations.iterator().next().getMessage());
    }
}
