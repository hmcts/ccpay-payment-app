package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.model.PaymentFee;

import java.util.List;

public interface DelegatingPaymentService<T, ID> {

    T create(PaymentServiceRequest paymentServiceRequest) throws CheckDigitException;

    T update(PaymentServiceRequest paymentServiceRequest) throws CheckDigitException;

    T retrieve(ID id);

    default T retrieveWithCallBack(ID id) {
        throw new UnsupportedOperationException();
    }

    T retrieve(ID id, String paymentTargetService);

    List<T> search(PaymentSearchCriteria searchCriteria);

}
