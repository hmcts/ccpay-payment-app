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

import static org.junit.Assert.assertEquals;

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

    @Test
    public void testAmountFractionInCardPaymentRequest(){
        CardPaymentRequest request = new CardPaymentRequest();
        request.setAmount(BigDecimal.valueOf(100.1234));
        Set<ConstraintViolation<CardPaymentRequest>> violations = validator.validate(request);
        violations.stream().forEach(v->{
                if(v.getMessage().equals("Payment amount cannot have more than 2 decimal places")){
                    assertEquals("Payment amount cannot have more than 2 decimal places",v.getMessage());
                }
            }
        );
    }

    @Test
    public void  testNegativeAmountInCardPaymentRequest(){
        CardPaymentRequest request = new CardPaymentRequest();
        request.setAmount(BigDecimal.valueOf(-100.12));
        Set<ConstraintViolation<CardPaymentRequest>> violations = validator.validate(request);
        violations.stream().forEach(v->{
                if(v.getMessage().equals("must be greater than 0")){
                    assertEquals("must be greater than 0",v.getMessage());
                }
            }
        );
    }

    @Test
    public void  testEmptyDescriptionInCardPaymentRequest(){
        CardPaymentRequest request = new CardPaymentRequest();
        request.setCcdCaseNumber("ccd-number");
        request.setSiteId("site-id");
        request.setDescription("");
        Set<ConstraintViolation<CardPaymentRequest>> violations = validator.validate(request);
        violations.stream().forEach(v->{
                if(v.getMessage().equals("must not be empty")){
                    assertEquals("must not be empty",v.getMessage());
                }
            }
        );
    }

    @Test
    public void testCcdnumberOrCaseReference(){
        CardPaymentRequest request = new CardPaymentRequest();
        request.setSiteId("site-id");
        request.setDescription("");
        Set<ConstraintViolation<CardPaymentRequest>> violations = validator.validate(request);
        violations.stream().forEach(v->{
                if(v.getMessage().equals("Either ccdCaseNumber or caseReference is required.")){
                    assertEquals("Either ccdCaseNumber or caseReference is required.",v.getMessage());
                }
            }
        );
    }

    @Test
    public void testValidateSiteIdForAdoption() {
        CreditAccountPaymentRequest request = new CreditAccountPaymentRequest();
        request.setService("ADOPTION");
        request.setSiteId("invalid-site-id");
        Set<ConstraintViolation<CreditAccountPaymentRequest>> violations = validator.validate(request);
        violations.stream().forEach(v->{
                if(v.getMessage().equals("Invalid Site ID (URN) provided for Adoption. Accepted values are ABA4")){
                    assertEquals("Invalid Site ID (URN) provided for Adoption. Accepted values are ABA4",v.getMessage());
                }
            }
        );
    }

    @Test
    public void testValidateSiteIdForPRL() {
        CreditAccountPaymentRequest request = new CreditAccountPaymentRequest();
        request.setService("PRL");
        request.setSiteId("invalid-site-id");
        Set<ConstraintViolation<CreditAccountPaymentRequest>> violations = validator.validate(request);
        violations.stream().forEach(v->{
                if(v.getMessage().equals("Invalid Site ID (URN) provided for PRL. Accepted values are ABA5")){
                    assertEquals("Invalid Site ID (URN) provided for FPL. Accepted values are ABA5",v.getMessage());
                }
            }
        );
    }


}
