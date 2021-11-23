package uk.gov.hmcts.payment.api.service;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.SupplementaryPaymentDto;
import java.util.List;
@Service
public interface IacService {
    ResponseEntity<SupplementaryPaymentDto> getIacSupplementaryInfo(List<PaymentDto> paymentDtos, String serviceName);
}
