package uk.gov.hmcts.payment.api.validators;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import static java.util.Optional.empty;

@Component
public class PaymentValidator {

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_TIME;
    public static final String ISO_MIDNIGHT = "00:00:00";

    public void validate(Optional<String> paymentMethodType, Optional<String> serviceType,
                         Optional<String> startDateString, Optional<String> endDateString) {
        validate(paymentMethodType, serviceType, startDateString, endDateString, Optional.of(ISO_MIDNIGHT), Optional.of(ISO_MIDNIGHT));
    }

    public void validate(Optional<String> paymentMethodType, Optional<String> serviceType,
                         Optional<String> startDateString, Optional<String> endDateString,
                         Optional<String> startTimeString, Optional<String> endTimeString) {
        ValidationErrorDTO dto = new ValidationErrorDTO();
        validateTypes(paymentMethodType, serviceType, dto);

        if (!startTimeString.isPresent() ^ !endTimeString.isPresent()) {
            dto.addFieldError("date_time",
                "In presence of start_time or end_time: start_date, end_date, start_time and end_time " +
                    "parameters need to be supplied");
        } else {
            if (!startTimeString.isPresent()) {
                startTimeString = Optional.of(ISO_MIDNIGHT);
                endTimeString = Optional.of(ISO_MIDNIGHT);
            }

            Optional<LocalDate> startDate = parseAndValidateDate(startDateString, "start_date", dto);
            Optional<LocalTime> startTime = parseAndValidateTime(startTimeString, "start_time", dto);
            Optional<LocalDate> endDate = parseAndValidateDate(endDateString, "end_date", dto);
            Optional<LocalTime> endTime = parseAndValidateTime(endTimeString, "end_time", dto);

            Optional<LocalDateTime> startDateTime = validateDateTime(startDate, startTime, "start_date_time", dto);
            Optional<LocalDateTime> endDateTime = validateDateTime(endDate, endTime, "end_date_time", dto);

            validateDateTimes(startDateTime, endDateTime, dto);
        }

        if (dto.hasErrors()) {
            throw new ValidationErrorException("Error occurred in the payment params", dto);
        }
    }

    private Optional<LocalDateTime> validateDateTime(Optional<LocalDate> date, Optional<LocalTime> time, String fieldName, ValidationErrorDTO dto) {
        if (date.isPresent() && time.isPresent()) {
            LocalDateTime dateTime = LocalDateTime.of(date.get(), time.get());
            checkFutureDateTime(dateTime, fieldName, dto);

            return Optional.of(dateTime);
        }

        return Optional.empty();
    }

    private void validateTypes(Optional<String> paymentMethodType, Optional<String> serviceType, ValidationErrorDTO dto) {
        if (paymentMethodType.isPresent() && !EnumUtils.isValidEnum(PaymentMethodType.class, paymentMethodType.get().toUpperCase())) {
            dto.addFieldError("payment_method", "Invalid payment method requested");
        }

        if (serviceType.isPresent() && !EnumUtils.isValidEnum(Service.class, serviceType.get().toUpperCase())) {
            dto.addFieldError("service_name", "Invalid service name requested");
        }
    }

    private void validateDateTimes(Optional<LocalDateTime> startDateTime, Optional<LocalDateTime> endDateTime, ValidationErrorDTO dto) {
        if (startDateTime.isPresent() && endDateTime.isPresent() && startDateTime.get().isAfter(endDateTime.get())) {
            dto.addFieldError("dates", "Start date cannot be greater than end date");
        }

        if (dto.hasErrors()) {
            throw new ValidationErrorException("Error occurred in the payment params", dto);
        }
    }

    private Optional<LocalTime> parseAndValidateTime(Optional<String> startTimeString, String fieldName, ValidationErrorDTO dto) {
        return startTimeString.flatMap(s -> validateTime(s, dto, fieldName));
    }

    private Optional<LocalDate> parseAndValidateDate(Optional<String> dateString, String fieldName, ValidationErrorDTO dto) {
        return dateString.flatMap(s -> validateDate(s, dto, fieldName));
    }

    private Optional<LocalTime> validateTime(String timeString, ValidationErrorDTO dto, String fieldName) {
        Optional<LocalTime> formattedTime = parseTimeFrom(timeString);
        if (!formattedTime.isPresent()) {
            dto.addFieldError(fieldName, "Invalid time format received");
        }

        return formattedTime;
    }

    private Optional<LocalDate> validateDate(String dateString, ValidationErrorDTO dto, String fieldName) {
        Optional<LocalDate> formattedDate = parseDateFrom(dateString);
        if (!formattedDate.isPresent()) {
            dto.addFieldError(fieldName, "Invalid date format received");
        }

        return formattedDate;
    }

    private void checkFutureDateTime(LocalDateTime dateTime, String fieldName, ValidationErrorDTO dto) {
        if (dateTime.isAfter(LocalDateTime.now())) {
            dto.addFieldError(fieldName, "Date cannot be in the future");
        }
    }

    private static Optional<LocalDate> parseDateFrom(String value) {
        try {
            return Optional.of(LocalDate.parse(value, dateFormatter));
        } catch (DateTimeParseException ex) {
            return empty();
        }
    }

    private static Optional<LocalTime> parseTimeFrom(String value) {
        try {
            return Optional.of(LocalTime.parse(value, timeFormatter));
        } catch (DateTimeParseException ex) {
            return empty();
        }
    }
}
