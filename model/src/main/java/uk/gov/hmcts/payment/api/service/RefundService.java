package uk.gov.hmcts.payment.api.service;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.dto.RefundReferenceDto;
import uk.gov.hmcts.payment.api.dto.RefundRequest;

public interface RefundService {
    RefundRequest createAndValidateRetroSpectiveRemissionRequest(String remissionReference);

    ResponseEntity<RefundReferenceDto> createRefundRequestForRetroRemission(MultiValueMap<String, String> headersMap, RefundRequest refundRequest);
}
