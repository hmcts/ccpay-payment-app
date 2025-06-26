package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class PciPalConfigurationException extends RuntimeException{
    public PciPalConfigurationException(String message) {
        super(message);
    }
}
