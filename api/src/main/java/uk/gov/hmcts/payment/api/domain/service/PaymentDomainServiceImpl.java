package uk.gov.hmcts.payment.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.reform.ccd.client.model.SearchCriteria;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentDomainServiceImpl implements PaymentDomainService{

    @Autowired
    private PaymentService<PaymentFeeLink, String> paymentService;

    @Override
    public Payment getPaymentByApportionment(FeePayApportion feePayApportion) {
        return paymentService.getPaymentById(feePayApportion.getPaymentId());
    }

    @Override
    public Payment getPaymentByApportionmentAndSearchCriteria(FeePayApportion feePayApportion, PaymentSearchCriteria searchCriteria) {
        List<Payment> searchedPayments  = paymentService.searchByCriteria(searchCriteria);
        List<Payment> filteredPayments = searchedPayments.stream().filter(payment1 -> payment1.getId()==feePayApportion.getPaymentId()).collect(Collectors.toList());
        if(filteredPayments.size()>0){
            return filteredPayments.get(0);
        }
        throw new PaymentNotFoundException("Payment not found for the apportionment");
    }

    public List<FeePayApportion> getFeePayApportionByPaymentId(Integer paymentId){
        return paymentService.findByPaymentId(paymentId);
    }
}
