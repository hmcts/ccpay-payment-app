package uk.gov.hmcts.payment.api.v1.model.govpay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GovPayAuthUtil {

    @Value("#{'${gov.pay.operational_services}'.split(',')}")
    private List<String> operationalServices;

    @Autowired
    private GovPayKeyRepository govPayKeyRepository;

    public String getServiceToken(String callingService, String paymentService) {
        if (!callingService.equals(paymentService) && !operationalServices.contains(callingService)) {
            return govPayKeyRepository.getKey(callingService);
        }

        return govPayKeyRepository.getKey(paymentService);
    }
}
