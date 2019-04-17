package uk.gov.hmcts.payment.referencedata.exception;

public class ReferenceDataNotFoundException  extends RuntimeException {
    public ReferenceDataNotFoundException() {}

    public ReferenceDataNotFoundException(String message) {
        super(message);
    }
}
