package uk.gov.hmcts.payment.api.domain.service;

import uk.gov.hmcts.payment.api.dto.ReconcilePaymentDto;

import java.util.List;
import java.util.Optional;

public interface PaymentDomainService {

    List<ReconcilePaymentDto> retrievePayments(Optional<String> startDateTimeString, Optional<String> endDateTimeString, Optional<String> paymentMethodType, Optional<String> serviceType, String pbaNumber, String ccdCaseNumber);
}
