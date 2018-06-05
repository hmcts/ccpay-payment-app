package uk.gov.hmcts.payment.api.contract;

import java.util.Iterator;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Positive;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        Iterator<ConstraintViolation<FeeDto>> iterator = violations.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getMessage().equals("must be greater than 0")) {
                assertTrue(true);
                return;
            }
        }
        assertTrue("Validation should catch a negative fee volume number", false);
    }

    @Test
    public void feeVolumeSetToZeroShouldFailValidation() {
        FeeDto feeDto = new FeeDto();
        feeDto.setVolume(0);

        Set<ConstraintViolation<FeeDto>> violations = validator.validate(feeDto);

        Iterator<ConstraintViolation<FeeDto>> iterator = violations.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getMessage().equals("must be greater than 0")) {
                assertTrue(true);
                return;
            }
        }

        assertTrue("Validation should catch fee volume set to zero", false);
    }


}
