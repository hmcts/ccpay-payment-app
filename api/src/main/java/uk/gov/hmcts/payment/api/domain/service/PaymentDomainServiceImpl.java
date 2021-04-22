package uk.gov.hmcts.payment.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentService;
@Service
public class PaymentDomainServiceImpl implements PaymentDomainService{

    @Autowired
    private PaymentService<PaymentFeeLink, String> paymentService;

    @Override
    public Payment getPaymentByApportionment(FeePayApportion feePayApportion) {
        return paymentService.getPaymentById(feePayApportion.getPaymentId());
    }
}
