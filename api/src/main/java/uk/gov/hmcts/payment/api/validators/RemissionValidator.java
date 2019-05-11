package uk.gov.hmcts.payment.api.validators;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;

import java.util.List;

@Component
public class RemissionValidator {
    @Autowired
    private ReferenceDataService<SiteDTO> referenceDataService;

    public void validate(RemissionRequest remissionRequest) {
        ValidationErrorDTO dto = new ValidationErrorDTO();

        if(StringUtils.isEmpty(remissionRequest.getCaseReference()) && StringUtils.isEmpty(remissionRequest.getCcdCaseNumber())) {
            dto.addFieldError("case_ref_ccd_case_num", "Empty case reference and ccd case number is not allowed");
        }

        if (remissionRequest.getHwfAmount().compareTo(remissionRequest.getFee().getCalculatedAmount()) == 1) {
            dto.addFieldError("hwf_amount_calculated_amount", "Hwf amount cannot be greater than calculated amount.");
        }

        List<SiteDTO> sites = referenceDataService.getSiteIDs();

        if (sites.stream().noneMatch(o -> o.getSiteID().equals(remissionRequest.getSiteId()))) {
            dto.addFieldError("site_id", "Invalid siteID: " + remissionRequest.getSiteId());
        }

        if (dto.hasErrors()) {
            throw new ValidationErrorException("Error occurred in the payment params", dto);
        }
    }
}
