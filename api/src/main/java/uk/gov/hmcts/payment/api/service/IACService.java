package uk.gov.hmcts.payment.api.service;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.SupplementaryDetailsResponse;
import uk.gov.hmcts.payment.api.dto.SupplementaryPaymentDto;
import java.util.List;

public interface IACService {
    ResponseEntity<SupplementaryPaymentDto> getIacSupplementaryInfo(List<PaymentDto> paymentDtos, String serviceName);
    ResponseEntity<SupplementaryDetailsResponse> getIacSupplementaryInfoResponse(List<String> iacCcdCaseNos);
}
