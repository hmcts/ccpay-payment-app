package uk.gov.hmcts.payment.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;

import java.util.List;
@Service
public class FeeDomainServiceImpl implements FeeDomainService{

    @Autowired
    private FeePayApportionService feePayApportionService;

    @Override
    public List<FeePayApportion> getFeePayApportionsByFee(PaymentFee fee) {
        return feePayApportionService.getFeePayApportionByFeeId(fee.getId());
    }
}
