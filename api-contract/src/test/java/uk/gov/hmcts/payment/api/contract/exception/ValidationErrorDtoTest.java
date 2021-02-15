package uk.gov.hmcts.payment.api.contract.exception;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class ValidationErrorDtoTest {

    @Test
    public void testAddingFieldErrors(){
        ValidationErrorDTO validationErrorDTO = new ValidationErrorDTO();
        validationErrorDTO.addFieldError("payment_method","Invalid payment method requested");
        validationErrorDTO.addFieldError("service_name","Invalid service name requested");
        assertEquals(2,validationErrorDTO.getFieldErrors().size());
    }

    @Test
    public void testHasErrorsWhenNoErrorsArePresent(){
        ValidationErrorDTO validationErrorDTO = new ValidationErrorDTO();
        validationErrorDTO.addFieldError("payment_method","Invalid payment method requested");
        validationErrorDTO.addFieldError("service_name","Invalid service name requested");
        assertEquals(true,validationErrorDTO.hasErrors());
    }

    @Test
    public void testHasErrorsWhenErrorsArePresent(){
        ValidationErrorDTO validationErrorDTO = new ValidationErrorDTO();
        validationErrorDTO.addFieldError("payment_method","Invalid payment method requested");
        validationErrorDTO.addFieldError("service_name","Invalid service name requested");
        assertEquals(true,validationErrorDTO.hasErrors());
    }

}
