package uk.gov.hmcts.payment.api.contract;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class ReplayCreditAccountPaymentRequest {

    @NotEmpty
    private String existingPaymentReference;

    @NotNull
    private CreditAccountPaymentRequest creditAccountPaymentRequest;
}
