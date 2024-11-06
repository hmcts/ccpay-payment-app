package uk.gov.hmcts.payment.api.contract;

import org.junit.Before;
import org.junit.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        assertThat(StreamSupport.stream(violations.spliterator(), false)
            .collect(Collectors.toList()))
            .extracting(ConstraintViolation::getMessage)
            .contains("must be greater than 0");
    }

    @Test
    public void feeVolumeSetToZeroShouldFailValidation() {
        FeeDto feeDto = new FeeDto();
        feeDto.setVolume(0);

        Set<ConstraintViolation<FeeDto>> violations = validator.validate(feeDto);

        Iterator<ConstraintViolation<FeeDto>> iterator = violations.iterator();
        assertThat(StreamSupport.stream(violations.spliterator(), false)
            .collect(Collectors.toList()))
            .extracting(ConstraintViolation::getMessage)
            .contains("must be greater than 0");
    }

}
