package uk.gov.hmcts.payment.api.service;

public interface CasePaymentDeletionService {

    void deletePaymentsByCcdCaseNumber(String ccdCaseNumber);
}
