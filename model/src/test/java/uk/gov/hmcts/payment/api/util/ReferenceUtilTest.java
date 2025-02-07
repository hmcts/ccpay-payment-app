package uk.gov.hmcts.payment.api.util;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReferenceUtilTest {

    private ReferenceUtil referenceUtil;
    private LuhnCheckDigit luhnCheckDigit;

    @BeforeEach
    public void setUp() {
        referenceUtil = new ReferenceUtil();
        luhnCheckDigit = new LuhnCheckDigit();
    }

    @Test
    public void getNextTest() throws CheckDigitException {
        String result = referenceUtil.getNext("test");
        assertEquals(24, result.length());
    }

    @Test
    public void luhnDigitValidateTest() throws CheckDigitException {
        String result = referenceUtil.getNext("test");
        String refNumberWithCheckDigit = result.substring(5).replace("-", "");

        assertTrue(luhnCheckDigit.isValid(refNumberWithCheckDigit));
    }

    @Test
    public void getNextWithPrefixRCTest() throws CheckDigitException {
        String result = referenceUtil.getNext("RC");
        String regex = "^RC-\\d{4}-\\d{4}-\\d{4}-\\d{4}$";
        Pattern pattern = Pattern.compile(regex);

        assertTrue(pattern.matcher(result).matches(), "The reference does not match the expected format");
    }
}
