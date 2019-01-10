package uk.gov.hmcts.payment.api.validators;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;

@Component
public class RemissionValidator {
    public void validate(RemissionRequest remissionRequest) {
        ValidationErrorDTO dto = new ValidationErrorDTO();

        if(StringUtils.isEmpty(remissionRequest.getCaseReference()) && StringUtils.isEmpty(remissionRequest.getCcdCaseNumber())) {
            dto.addFieldError("case_ref_ccd_case_num", "Empty case reference and ccd case number is not allowed");
        }

        if (dto.hasErrors()) {
            throw new ValidationErrorException("Error occurred in the payment params", dto);
        }
    }
}
