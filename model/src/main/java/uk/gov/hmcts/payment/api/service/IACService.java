package uk.gov.hmcts.payment.api.service;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.payment.api.dto.SupplementaryDetailsResponse;

import java.util.List;

public interface IACService {
    ResponseEntity<SupplementaryDetailsResponse> getIacSupplementaryInfo(List<String> iacCcdCaseNos,String serviceToken);
}
