package uk.gov.hmcts.payment.api.v1.model.govpay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class GovPayAuthUtil {

    @Value("#{'${gov.pay.operational_services}'.split(',')}")
    private List<String> operationalServices;

    @Autowired
    private GovPayKeyRepository govPayKeyRepository;

    public String getServiceName(final String callingService, final String paymentService) {
        return Optional.ofNullable(callingService)
            .filter(p -> !p.equals(paymentService))
            .filter(p -> !operationalServices.contains(p))
            .orElse(paymentService);
    }

    public String getServiceToken(String serviceName) {
        return govPayKeyRepository.getKey(serviceName);
    }
}
