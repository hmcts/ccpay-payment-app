package uk.gov.hmcts.payment.api.validators;

import org.junit.Test;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;

import java.time.LocalDate;
import java.util.Optional;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

public class PaymentValidatorTest {

    private static String NOW_STRING = LocalDate.now().format(ISO_DATE);
    private static String FUTURE_STRING = LocalDate.now().plusDays(1).format(ISO_DATE);

    private PaymentValidator validator = new PaymentValidator();

    @Test
    public void shouldReturnNoErrors() {
        // no exception expected, none thrown: passes.
        validator.validate(Optional.of("PBA"), Optional.of("CMC"), Optional.of(NOW_STRING), Optional.of(NOW_STRING));
    }

    @Test
    public void shouldReturnNoErrorsWhenDatesEmpty() {
        // no exception expected, none thrown: passes.
        validator.validate(Optional.of("PBA"), Optional.of("CMC"), Optional.empty(), Optional.empty());
    }

    @Test
    public void shouldThrowValidationExceptionWithPaymentMethodError() {

        Throwable thrown = catchThrowable(() -> { validator.validate(Optional.of("UNKNOWN"), Optional.of("CMC"), Optional.of(NOW_STRING), Optional.of(NOW_STRING)); });

        assertThat(thrown).isInstanceOf(ValidationErrorException.class);
        ValidationErrorException ex = (ValidationErrorException) thrown;

        assertThat(ex.getMessage()).isEqualTo("Error occurred in the payment params");
        assertThat(ex.getErrors().hasErrors()).isTrue();
        assertThat(ex.getErrors().getFieldErrors().get(0).getField()).isEqualTo("payment_method");
        assertThat(ex.getErrors().getFieldErrors().get(0).getMessage()).isEqualTo("Invalid payment method requested");
    }

    @Test
    public void shouldThrowValidationExceptionWithServiceNameError() {

        Throwable thrown = catchThrowable(() -> { validator.validate(Optional.of("CARD"), Optional.of("UNKNOWN"), Optional.of(NOW_STRING), Optional.of(NOW_STRING)); });

        assertThat(thrown).isInstanceOf(ValidationErrorException.class);
        ValidationErrorException ex = (ValidationErrorException) thrown;

        assertThat(ex.getMessage()).isEqualTo("Error occurred in the payment params");
        assertThat(ex.getErrors().hasErrors()).isTrue();
        assertThat(ex.getErrors().getFieldErrors().get(0).getField()).isEqualTo("service_name");
        assertThat(ex.getErrors().getFieldErrors().get(0).getMessage()).isEqualTo("Invalid service name requested");
    }

    @Test
    public void shouldThrowValidationExceptionWithFutureStartDateError() {

        Throwable thrown = catchThrowable(() -> { validator.validate(Optional.of("PBA"), Optional.of("CMC"), Optional.of(FUTURE_STRING), Optional.of(NOW_STRING)); });

        assertThat(thrown).isInstanceOf(ValidationErrorException.class);
        ValidationErrorException ex = (ValidationErrorException) thrown;

        assertThat(ex.getMessage()).isEqualTo("Error occurred in the payment params");
        assertThat(ex.getErrors().hasErrors()).isTrue();
        assertThat(ex.getErrors().getFieldErrors().get(0).getField()).isEqualTo("start_date");
        assertThat(ex.getErrors().getFieldErrors().get(0).getMessage()).isEqualTo("Date cannot be in the future");
    }

    @Test
    public void shouldThrowValidationExceptionWithFutureEndDateError() {

        Throwable thrown = catchThrowable(() -> { validator.validate(Optional.of("PBA"), Optional.of("CMC"), Optional.of(NOW_STRING), Optional.of(FUTURE_STRING)); });

        assertThat(thrown).isInstanceOf(ValidationErrorException.class);
        ValidationErrorException ex = (ValidationErrorException) thrown;

        assertThat(ex.getErrors().hasErrors()).isTrue();
        assertThat(ex.getErrors().getFieldErrors().get(0).getField()).isEqualTo("end_date");
        assertThat(ex.getErrors().getFieldErrors().get(0).getMessage()).isEqualTo("Date cannot be in the future");
    }

    @Test
    public void shouldThrowValidationExceptionWhenStartDateIsGreaterThanEndDate() {

        Throwable thrown = catchThrowable(() -> { validator.validate(Optional.of("PBA"), Optional.of("CMC"), Optional.of(NOW_STRING), Optional.of(LocalDate.now().minusDays(1).format(ISO_DATE))); });

        assertThat(thrown).isInstanceOf(ValidationErrorException.class);
        ValidationErrorException ex = (ValidationErrorException) thrown;

        assertThat(ex.getMessage()).isEqualTo("Error occurred in the payment params");
        assertThat(ex.getErrors().hasErrors()).isTrue();
        assertThat(ex.getErrors().getFieldErrors().get(0).getField()).isEqualTo("dates");
        assertThat(ex.getErrors().getFieldErrors().get(0).getMessage()).isEqualTo("Start date cannot be greater than end date");
    }
}
