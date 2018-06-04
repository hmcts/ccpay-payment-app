package uk.gov.hmcts.payment.api.contract;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

public class FeeDtoTest {
    private static Validator validator;

    @Before
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void negativeFeeVolumeShouldFailValidation() {
        FeeDto feeDto = new FeeDto();
        feeDto.setVolume(-1);

        Set<ConstraintViolation<FeeDto>> violations = validator.validate(feeDto);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void feeVolumeSetToZeroShouldFailValidation() {
        FeeDto feeDto = new FeeDto();
        feeDto.setVolume(0);

        Set<ConstraintViolation<FeeDto>> violations = validator.validate(feeDto);
        assertFalse(violations.isEmpty());
    }


}
