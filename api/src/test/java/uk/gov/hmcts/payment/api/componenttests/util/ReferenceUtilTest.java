package uk.gov.hmcts.payment.api.componenttests.util;

import org.apache.commons.validator.routines.checkdigit.*;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;

import static org.junit.Assert.*;

public class ReferenceUtilTest {

    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";

    private ReferenceUtil referenceUtil;
    private CheckDigit checkDigit;

    @Before
    public void setUp() {
        referenceUtil = new ReferenceUtil();
        checkDigit = new LuhnCheckDigit();
    }


    @Test
    public void generatePaymentReference_isValidCheckDigit_shouldReturnTrueTest() throws Exception {

        for (int i = 0; i < 100; i++) {
            String ref = referenceUtil.getNext("RC");
            String refNumberWithCheckDigit = ref.substring(3, ref.length()).replace("-", "");

            assertTrue(ref.matches(PAYMENT_REFERENCE_REGEX));
            assertTrue(checkDigit.isValid(refNumberWithCheckDigit));
        }
    }

    @Test
    public void appendRandomDigit_inPlaceOfCheckDigit_isValidCheckDigit_shouldReturnFalseTest() throws Exception {
        String ref = referenceUtil.getNext("RC");
        String refNum = ref.substring(3, ref.length() - 1).replace("-", "");
        refNum = refNum + 7;

        assertTrue(ref.matches(PAYMENT_REFERENCE_REGEX));

        if (!checkDigit.isValid(refNum)) {
            assertFalse(checkDigit.isValid(refNum));
        }
    }
}

