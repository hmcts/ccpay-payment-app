package uk.gov.hmcts.payment.api.service;

public record PaymentDeletionAuditDetails(
    String ccdCaseNumber,
    String sourceSystem,
    String errorMessage,
    boolean success
) {
}
