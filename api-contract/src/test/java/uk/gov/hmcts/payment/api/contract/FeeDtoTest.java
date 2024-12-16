package uk.gov.hmcts.payment.api.contract;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.Before;
import org.junit.Test;

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
        assertThat(violations.stream().toList())
            .extracting(ConstraintViolation::getMessage)
            .contains("must be greater than 0");
    }

    @Test
    public void feeVolumeSetToZeroShouldFailValidation() {
        FeeDto feeDto = new FeeDto();
        feeDto.setVolume(0);

        Set<ConstraintViolation<FeeDto>> violations = validator.validate(feeDto);

        assertThat(violations.stream().toList())
            .extracting(ConstraintViolation::getMessage)
            .contains("must be greater than 0");
    }

}
