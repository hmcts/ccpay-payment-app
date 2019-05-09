package uk.gov.hmcts.payment.api.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;

@Service
public class PaymentGroupServiceImpl implements PaymentGroupService<PaymentFeeLink, String> {

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;

    public PaymentGroupServiceImpl(PaymentFeeLinkRepository paymentFeeLinkRepository) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
    }

    @Override
    public PaymentFeeLink findByPaymentGroupReference(String paymentGroupReference) {
        return paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference).orElseThrow(InvalidPaymentGroupReferenceException::new);
    }
}
