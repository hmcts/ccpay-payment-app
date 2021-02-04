package uk.gov.hmcts.payment.api.contract.util;

import org.junit.Test;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;

import javax.validation.constraints.AssertFalse;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LanguageTest {

    @Test
    public void testLanguageCode(){
        assertEquals("cy",Language.CY.getLanguage());
    }

    @Test
    public void testEmtpyLanguageCode(){
        CardPaymentRequest request = CardPaymentRequest.createCardPaymentRequestDtoWith()
                                        .language("").build();
        assertEquals("",request.getLanguage());
    }
}
