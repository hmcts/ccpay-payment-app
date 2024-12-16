package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.hc.core5.http.MethodNotSupportedException;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.List;

public interface DelegatingPaymentService<T, ID> {

    T create(PaymentServiceRequest paymentServiceRequest) throws CheckDigitException;

    T create(CreatePaymentRequest createPaymentRequest, String serviceName);

    void cancel(Payment payment, String ccdCaseNumber);

    void cancel(Payment payment, String ccdCaseNumber, String serviceName);

    T update(PaymentServiceRequest paymentServiceRequest) throws CheckDigitException, MethodNotSupportedException;

    T retrieve(ID id);

    T retrieve(PaymentFeeLink paymentFeeLink, ID id);

    default T retrieveWithCallBack(ID id) {
        throw new UnsupportedOperationException();
    }

    T retrieve(ID id, String paymentTargetService);

    List<T> search(PaymentSearchCriteria searchCriteria);

    void cancel(String cancelUrl);

    void cancel(String cancelUrl, String serviceName);

    List<Payment> searchByCriteria(PaymentSearchCriteria searchCriteria);



}
