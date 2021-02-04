package uk.gov.hmcts.payment.api.contract.exception;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FieldErrorDtoTest {

    FieldErrorDTO validationErrorDto;

    @Before
    public void beforeEach() {
        validationErrorDto = new FieldErrorDTO("payment_method","Invalid payment method requested");
    }

    @Test
    public void testValuesInFieldError(){
        assertEquals("payment_method",validationErrorDto.getField());
    }

    @Test
    public void testValuesInMessageError(){
        assertEquals("Invalid payment method requested",validationErrorDto.getMessage());
    }

}
