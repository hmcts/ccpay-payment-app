package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.dto.FeePayApportionCCDCase;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.List;

public interface FeePayApportionService<T, ID> {

    List<FeePayApportionCCDCase> findAllHistoricalCases();

    T findFeePayGroupsByCase(String ccdCase);

    T findFeesByFeePayGroup(PaymentFeeLink feePayGroup);

    T findRemissionsByFeePayGroup(PaymentFeeLink feePayGroup);

    T findPaymentsByFeePayGroup(PaymentFeeLink feePayGroup);

    FeePayApportionCCDCase processFeePayApportion(FeePayApportionCCDCase feePayApportionCCDCase);

}
