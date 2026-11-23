package uk.gov.hmcts.payment.api.service;

public interface BarPaymentDeletionService {

    void deleteBarPaymentsByCcdCaseNumber(String ccdCaseNumber);
}
