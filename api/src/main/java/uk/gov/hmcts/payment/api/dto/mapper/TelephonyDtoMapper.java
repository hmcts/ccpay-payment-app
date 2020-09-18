package uk.gov.hmcts.payment.api.dto.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.TelephonyDto;
import uk.gov.hmcts.payment.api.external.client.dto.PCIPALAntennaResponse;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;

@Component
public class TelephonyDtoMapper {

    public TelephonyDto toPciPalAntennaCardPaymentDto(PaymentFeeLink paymentFeeLink, Payment payment, PCIPALAntennaResponse pcipalAntennaResponse) {
        return TelephonyDto.telephonyDtoWith()
            .status(PayStatusToPayHubStatus.valueOf(payment.getStatus().toLowerCase()).getMappedStatus())
            .paymentReference(payment.getReference())
            .paymentGroupReference(paymentFeeLink.getPaymentReference())
            .dateCreated(payment.getDateCreated())
            .links(new TelephonyDto.LinksDto(new TelephonyDto.LinkDto(pcipalAntennaResponse.getNextUrl(), "POST",pcipalAntennaResponse.getAccessToken(),pcipalAntennaResponse.getRefreshToken())))
            .build();
    }
}
