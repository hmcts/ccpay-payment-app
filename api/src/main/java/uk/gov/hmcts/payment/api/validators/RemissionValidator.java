package uk.gov.hmcts.payment.api.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;

import java.util.List;

@Component
public class RemissionValidator {

    @Autowired
    private ReferenceDataService<SiteDTO> referenceDataService;

    public void validate(RemissionRequest remissionRequest) {
        ValidationErrorDTO dto = new ValidationErrorDTO();

        List<SiteDTO> sites = referenceDataService.getSiteIDs();

        if (sites.stream().noneMatch(o -> o.getSiteID().equals(remissionRequest.getSiteId()))) {
            dto.addFieldError("site_id", "Invalid siteID: " + remissionRequest.getSiteId());
        }

        if (dto.hasErrors()) {
            throw new ValidationErrorException("Error occurred in the payment params", dto);
        }
    }
}
