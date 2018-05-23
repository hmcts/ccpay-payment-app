package uk.gov.hmcts.payment.api.validators;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.util.PaymentMethodUtil;

import java.time.LocalDate;

@Component
public class PaymentValidator {

    public void validate(String paymentMethodType, LocalDate startDate, LocalDate endDate) {
        ValidationErrorDTO dto = new ValidationErrorDTO();
        if (!EnumUtils.isValidEnum(PaymentMethodUtil.class, paymentMethodType.toUpperCase())) {
            dto.addFieldError("payment_method", "Invalid payment method requested");
        }
        checkFutureDate(startDate, "start_date", dto);
        checkFutureDate(endDate, "end_date", dto);

        if (startDate.isAfter(endDate)) {
            dto.addFieldError("dates", "Start date cannot be greater than end date");
        }

        if (dto.hasErrors()) {
            throw new ValidationErrorException("Error occurred in the payment params", dto);
        }
    }

    private void checkFutureDate(LocalDate date, String fieldName, ValidationErrorDTO dto) {
        if (date.isAfter(LocalDate.now())) {
            dto.addFieldError(fieldName, "Date cannot be in the future");
        }
    }

}
