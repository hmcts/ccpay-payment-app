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

import static org.assertj.core.api.Assertions.assertThat;

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

    private static Set<ConstraintViolation<TelephonyCardPaymentsRequest>> violationsForAmount(String amount) {
        TelephonyCardPaymentsRequest request = new TelephonyCardPaymentsRequest();
        request.setAmount(new BigDecimal(amount));
        return validator.validateProperty(request, "amount");
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

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    public void testAmountWithTwoDecimalPlacesIsValid() {
        assertThat(violationsForAmount("2176.64")).isEmpty();
    }

    @Test
    public void testAmountWithZeroDecimalPlacesIsValid() {
        assertThat(violationsForAmount("2176")).isEmpty();
    }

    @Test
    public void testAmountWithOneDecimalPlaceIsValid() {
        assertThat(violationsForAmount("2176.6")).isEmpty();
    }

    @Test
    public void testInvalidAmount() {
        assertThat(violationsForAmount("-2176.64"))
            .extracting(ConstraintViolation::getMessage)
            .containsExactlyInAnyOrder("must be greater than 0", "must be greater than or equal to 0.01");
    }

    @Test
    public void testAmountWithMoreThanTwoDecimalPlaces() {
        TelephonyCardPaymentsRequest request = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("2176.6400000000003"))
            .ccdCaseNumber("1234567890123456")
            .caseType("case-type")
            .currency(CurrencyCode.GBP)
            .returnURL("http://example.com")
            .telephonySystem("KERV")
            .build();

        assertThat(validator.validate(request))
            .hasSize(1)
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("Payment amount cannot have more than 2 decimal places");
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

        assertThat(validator.validate(request))
            .hasSize(1)
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("ccd_case_number length must be 16 digits");
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

        assertThat(validator.validate(request))
            .hasSize(1)
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("must not be blank");
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

        assertThat(validator.validate(request))
            .hasSize(1)
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("must not be null");
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

        assertThat(validator.validate(request))
            .hasSize(1)
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("must not be empty");
    }
}
