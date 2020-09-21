package uk.gov.hmcts.payment.api.dto.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.TelephonyCardPaymentsResponse;
import uk.gov.hmcts.payment.api.external.client.dto.PCIPALResponse;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;

@Component
public class TelephonyDtoMapper {

    public TelephonyCardPaymentsResponse toPciPalTelephonyCardPaymentsDto(PaymentFeeLink paymentFeeLink, Payment payment, PCIPALResponse pcipalResponse) {
        return TelephonyCardPaymentsResponse.telephonyDtoWith()
            .status(PayStatusToPayHubStatus.valueOf(payment.getStatus().toLowerCase()).getMappedStatus())
            .paymentReference(payment.getReference())
            .paymentGroupReference(paymentFeeLink.getPaymentReference())
            .dateCreated(payment.getDateCreated())
            .links(new TelephonyCardPaymentsResponse.LinksDto(new TelephonyCardPaymentsResponse.LinkDto(pcipalResponse.getNextUrl(), "POST", pcipalResponse.getAccessToken(), pcipalResponse.getRefreshToken())))
            .build();
    }
}
