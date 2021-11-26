package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.model.PaymentFee;

import java.util.Optional;

public interface FeesService{
    void deleteFee(int feeId);

    Optional<PaymentFee> getPaymentFee(int feeId);
}

