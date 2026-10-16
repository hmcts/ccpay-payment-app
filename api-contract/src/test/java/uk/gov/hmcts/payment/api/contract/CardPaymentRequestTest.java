package uk.gov.hmcts.payment.api.contract;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CardPaymentRequestTest {
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

    private static Set<ConstraintViolation<CardPaymentRequest>> violationsForAmount(String amount) {
        CardPaymentRequest request = new CardPaymentRequest();
        request.setAmount(new BigDecimal(amount));
        return validator.validateProperty(request, "amount");
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
    public void testAmountWithMoreThanTwoDecimalPlacesIsInvalid() {
        assertThat(violationsForAmount("2176.6400000000003"))
            .hasSize(1)
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("Payment amount cannot have more than 2 decimal places");
    }

    @Test
    public void testNegativeAmountIsInvalid() {
        assertThat(violationsForAmount("-2176.64"))
            .extracting(ConstraintViolation::getMessage)
            .containsExactlyInAnyOrder("must be greater than 0", "must be greater than or equal to 0.01");
    }

    @Test
    public void  testEmptyDescriptionInCardPaymentRequest(){
        CardPaymentRequest request = new CardPaymentRequest();
        request.setCcdCaseNumber("ccd-number");
        request.setSiteId("site-id");
        request.setDescription("");

        assertThat(validator.validate(request))
            .extracting(ConstraintViolation::getMessage)
            .contains("must not be empty");
    }

    @Test
    public void testCcdnumberOrCaseReference(){
        CardPaymentRequest request = new CardPaymentRequest();
        request.setSiteId("site-id");
        request.setDescription("");

        assertThat(validator.validate(request))
            .extracting(ConstraintViolation::getMessage)
            .contains("Either ccdCaseNumber or caseReference is required.");
    }

}
