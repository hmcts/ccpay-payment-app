package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.Payment;

import java.util.Date;
import java.util.List;

public interface PaymentService<T, ID> {

    T retrievePayment(ID id);

    List<Payment> retrievePayment(List<ID> id);

    List<Reference> listInitiatedStatusPaymentsReferences();

    List<T> search(PaymentSearchCriteria searchCriteria);

    void updateTelephonyPaymentStatus(String reference, String status, String payload);

    List<Payment> getPayments(Date atStartOfDay, Date atEndOfDay);

    List<FeePayApportion> findByPaymentId(Integer paymentId);

    List<Payment> searchByCriteria(PaymentSearchCriteria searchCriteria);

    String getServiceNameByCode(String serviceCode);

    Payment getPaymentById(Integer id);

    Payment findSavedPayment(String reference);

    void updatePaymentsForCCDCaseNumberByCertainDays(String ccd_case_number, String days);

    Payment findPayment(String internalReference);

}
