package uk.gov.hmcts.payment.api.contract.exception;

import org.junit.Test;




public class ValidationErrorDtoTest {

    @Test
    public void testAddingFieldErrors(){
        ValidationErrorDTO validationErrorDTO = new ValidationErrorDTO();
        validationErrorDTO.addFieldError("payment_method","Invalid payment method requested");
        validationErrorDTO.addFieldError("service_name","Invalid service name requested");

        
    }

}
