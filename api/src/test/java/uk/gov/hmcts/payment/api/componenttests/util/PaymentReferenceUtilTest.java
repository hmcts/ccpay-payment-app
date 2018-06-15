package uk.gov.hmcts.payment.api.componenttests.util;

import org.apache.commons.validator.routines.checkdigit.*;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.payment.api.util.PaymentReferenceUtil;

import static org.junit.Assert.*;

public class PaymentReferenceUtilTest {

    private final static String PAYMENT_REFERENCE_REFEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4}){1}";

    private PaymentReferenceUtil paymentReferenceUtil;
    private CheckDigit checkDigit;

    @Before
    public void setUp() {
        paymentReferenceUtil = new PaymentReferenceUtil();
        checkDigit = new LuhnCheckDigit();
    }


    @Test
    public void generatePaymentReference_isValidCheckDigit_shouldReturnTrueTest() throws Exception {

        for (int i=0 ; i<100 ; i++) {
            String ref = paymentReferenceUtil.getNext();
            String refNumberWithCheckDigit = ref.substring(3, ref.length()).replace("-", "");

            assertTrue(ref.matches(PAYMENT_REFERENCE_REFEX));
            assertTrue(checkDigit.isValid(refNumberWithCheckDigit));
        }
    }

    @Test
    public void appendRandomDigit_inPlaceOfCheckDigit_isValidCheckDigit_shouldReturnFalseTest() throws Exception {
        String ref = paymentReferenceUtil.getNext();
        String refNum = ref.substring(3, ref.length()-1).replace("-", "");
        refNum = refNum + 7;

        assertTrue(ref.matches(PAYMENT_REFERENCE_REFEX));

        if (!checkDigit.isValid(refNum)) {
            assertFalse(checkDigit.isValid(refNum));
        }
    }
}

