package uk.gov.hmcts.payment.api.validators;

import org.apache.commons.lang3.EnumUtils;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.util.DateUtil;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.time.format.DateTimeParseException;
import java.util.Optional;

import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class PaymentValidator {

    private static final Logger LOG = getLogger(PaymentValidator.class);
    private final DateUtil dateUtil;

    @Autowired
    public PaymentValidator(DateUtil dateUtil) {
        this.dateUtil = dateUtil;
    }

    public void validate(Optional<String> paymentMethodType, Optional<String> startDateString, Optional<String> endDateString) {
        LOG.info("Inside validate");
        ValidationErrorDTO dto = new ValidationErrorDTO();

        if (paymentMethodType.isPresent() && !EnumUtils.isValidEnum(PaymentMethodType.class, paymentMethodType.get().toUpperCase())) {
            dto.addFieldError("payment_method", "Invalid payment method requested");
        }

        Optional<LocalDateTime> startDate = parseAndValidateDate(startDateString, "start_date", dto);
        Optional<LocalDateTime> endDate = parseAndValidateDate(endDateString, "end_date", dto);

        if (startDate.isPresent() && endDate.isPresent() && startDate.get().isAfter(endDate.get())) {
            dto.addFieldError("dates", "Start date cannot be greater than end date");
        }

        if (dto.hasErrors()) {
            LOG.info("Validation error exists: {}", dto);
            throw new ValidationErrorException("Error occurred in the payment params", dto);
        }
        LOG.info("Validation is successful");
    }

    public void validateToFromDates(Optional<String> startDateString, Optional<String> endDateString) {
        LOG.info("Inside validate");
        ValidationErrorDTO dto = new ValidationErrorDTO();

        Optional<LocalDateTime> startDate = parseAndValidateDate(startDateString, "start_date", dto);
        Optional<LocalDateTime> endDate = parseAndValidateDate(endDateString, "end_date", dto);

        if (startDate.isPresent() && endDate.isPresent() && startDate.get().isAfter(endDate.get())) {
            dto.addFieldError("dates", "Start date cannot be greater than end date");
        }

        if (dto.hasErrors()) {
            LOG.info("Validation error exists: {}", dto);
            throw new ValidationErrorException("Error occurred in the payment params", dto);
        }
        LOG.info("Validation is successful");
    }

    private Optional<LocalDateTime> parseAndValidateDate(Optional<String> dateTimeString, String fieldName, ValidationErrorDTO dto) {
        return dateTimeString.flatMap(s -> validateDate(s, dto, fieldName));
    }

    private Optional<LocalDateTime> validateDate(String dateString, ValidationErrorDTO dto, String fieldName) {
        Optional<LocalDateTime> formattedDate = parseFrom(dateString);
        if (!formattedDate.isPresent()) {
            dto.addFieldError(fieldName, "Invalid date format received, required data format is ISO");
        } else {
            checkFutureDate(formattedDate.get(), fieldName, dto);
        }
        return formattedDate;
    }
    private void checkFutureDate(LocalDateTime date, String fieldName, ValidationErrorDTO dto) {
        if (date.isAfter(LocalDateTime.now())) {
            dto.addFieldError(fieldName, "Date cannot be in the future");
        }
    }

    private Optional<LocalDateTime> parseFrom(String value) {
        try {
            return Optional.of(LocalDateTime.parse(value, dateUtil.getIsoDateTimeFormatter()));
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            return empty();
        }
    }
}
