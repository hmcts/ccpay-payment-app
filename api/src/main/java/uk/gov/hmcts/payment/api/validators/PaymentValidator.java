package uk.gov.hmcts.payment.api.validators;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.util.PaymentMethodUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import static java.util.Optional.empty;

@Component
public class PaymentValidator {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;

    public void validate(String paymentMethodType, Optional<String> startDateString, Optional<String> endDateString) {
        ValidationErrorDTO dto = new ValidationErrorDTO();
        if (!EnumUtils.isValidEnum(PaymentMethodUtil.class, paymentMethodType.toUpperCase())) {
            dto.addFieldError("payment_method", "Invalid payment method requested");
        }

        Optional<LocalDate> startDate = parseAndValidateDate(startDateString, "start_date", dto);
        Optional<LocalDate> endDate = parseAndValidateDate(endDateString, "end_date", dto);

        if (startDate.isPresent() && endDate.isPresent() && startDate.get().isAfter(endDate.get())) {
            dto.addFieldError("dates", "Start date cannot be greater than end date");
        }

        if (dto.hasErrors()) {
            throw new ValidationErrorException("Error occurred in the payment params", dto);
        }
    }

    private Optional<LocalDate> parseAndValidateDate(Optional<String> startDateString, String fieldName, ValidationErrorDTO dto) {
        return startDateString.flatMap(s -> validateDate(s, dto, fieldName));
    }

    private Optional<LocalDate> validateDate(String dateString, ValidationErrorDTO dto, String fieldName) {
        Optional<LocalDate> formattedDate = parseFrom(dateString);
        if (!formattedDate.isPresent()) {
            dto.addFieldError(fieldName, "Invalid date format received");
        } else {
            checkFutureDate(formattedDate.get(), fieldName, dto);
        }
        return formattedDate;
    }
    private void checkFutureDate(LocalDate date, String fieldName, ValidationErrorDTO dto) {
        if (date.isAfter(LocalDate.now())) {
            dto.addFieldError(fieldName, "Date cannot be in the future");
        }
    }

    private static Optional<LocalDate> parseFrom(String value) {
        try {
            return Optional.of(LocalDate.parse(value,formatter));
        } catch (DateTimeParseException ex) {
            return empty();
        }
    }
}
