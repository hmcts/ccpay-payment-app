package uk.gov.hmcts.payment.api.contract;

import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Iterator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(iterator).extracting(ConstraintViolation::getMessage)
            .contains("must be greater than 0");
    }

    @Test
    public void feeVolumeSetToZeroShouldFailValidation() {
        FeeDto feeDto = new FeeDto();
        feeDto.setVolume(0);

        Set<ConstraintViolation<FeeDto>> violations = validator.validate(feeDto);

        Iterator<ConstraintViolation<FeeDto>> iterator = violations.iterator();
        assertThat(iterator).extracting(ConstraintViolation::getMessage)
            .contains("must be greater than 0");
    }


}
