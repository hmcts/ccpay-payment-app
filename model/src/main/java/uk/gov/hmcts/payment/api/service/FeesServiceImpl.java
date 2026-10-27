package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.util.ServiceRequests;

import java.util.Optional;


@Service
public class FeesServiceImpl implements FeesService {
    private static final Logger LOG = LoggerFactory.getLogger(FeesServiceImpl.class);
    private final PaymentFeeRepository paymentFeeRepository;
    private final PaymentFeeLinkRepository paymentFeeLinkRepository;

    public FeesServiceImpl(PaymentFeeRepository paymentFeeRepository, PaymentFeeLinkRepository paymentFeeLinkRepository) {
        this.paymentFeeRepository = paymentFeeRepository;
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
    }

    @Override
    @Transactional
    public void deleteFee(int feeId) throws EmptyResultDataAccessException {
        Optional<PaymentFee> paymentFee = getPaymentFee(feeId);
        if (paymentFee.isEmpty()) {
            throw new EmptyResultDataAccessException("Fee not found", 1);
        }
        PaymentFeeLink serviceRequest = paymentFeeLinkRepository
            .findByIdForUpdate(paymentFee.get().getPaymentLink().getId())
            .orElseThrow(() -> new EmptyResultDataAccessException("Service request not found", 1));
        // TODO: Confirm this should be blocked when the service request is cancelled.
        ServiceRequests.requireNotCancelled(serviceRequest);
        paymentFeeRepository.deleteById(feeId);
    }

    @Override
    public Optional<PaymentFee> getPaymentFee(int feeId) {
        return paymentFeeRepository.findById(feeId);
    }

}
