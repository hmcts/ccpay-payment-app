package uk.gov.hmcts.payment.api.util;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.ModulusCheckDigit;

public class CheckDigitUtil extends ModulusCheckDigit{

    public CheckDigitUtil(int modulus) {
        super(modulus);
    }

    @Override
    public String calculate(String code) throws CheckDigitException {
        return super.calculate(code);
    }

    @Override
    public boolean isValid(String code) {
        return super.isValid(code);
    }

    @Override
    protected int weightedValue(int i, int i1, int i2) throws CheckDigitException {
        // odd or even position
        return 7;
    }

    @Override
    public int calculateModulus(String code,
                                boolean includesCheckDigit) throws CheckDigitException {
        return super.calculateModulus(code, includesCheckDigit);
    }
}
