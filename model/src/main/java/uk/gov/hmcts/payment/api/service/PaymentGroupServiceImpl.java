package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;

@Service
public class PaymentGroupServiceImpl implements PaymentGroupService<PaymentFeeLink, String> {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentGroupServiceImpl.class);

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;

    public PaymentGroupServiceImpl(PaymentFeeLinkRepository paymentFeeLinkRepository) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
    }

    @Override
    public PaymentFeeLink findByPaymentGroupReference(String paymentGroupReference) {
        return paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference).orElseThrow(InvalidPaymentGroupReferenceException::new);
    }

    @Override
    public PaymentFeeLink addNewFeeWithPaymentGroup(PaymentFee fee, String paymentGroupReference) {
        PaymentFeeLink feeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(paymentGroupReference)
            .fees(Lists.newArrayList(fee))
            .build();
        fee.setPaymentLink(feeLink);

        return paymentFeeLinkRepository.save(feeLink);
    }

    @Override
    @Transactional
    public PaymentFeeLink addNewFeetoExistingPaymentGroup(PaymentFee fee, String paymentGroupReference) {

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference)
            .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Payment group " + paymentGroupReference + " does not exists."));

        paymentFeeLink.getFees().add(fee);

        fee.setPaymentLink(paymentFeeLink);

        return paymentFeeLink;
    }
}
