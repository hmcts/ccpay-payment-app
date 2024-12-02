package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;

import java.util.Optional;


@Service
public class FeesServiceImpl implements FeesService {
    private static final Logger LOG = LoggerFactory.getLogger(FeesServiceImpl.class);
    private final PaymentFeeRepository paymentFeeRepository;

    public FeesServiceImpl(PaymentFeeRepository paymentFeeRepository) {
        this.paymentFeeRepository = paymentFeeRepository;
    }

    @Override
    public void deleteFee(int feeId) throws EmptyResultDataAccessException {
        paymentFeeRepository.deleteById(feeId);
    }

    @Override
    public Optional<PaymentFee> getPaymentFee(int feeId) {
        return paymentFeeRepository.findById(feeId);
    }

}
