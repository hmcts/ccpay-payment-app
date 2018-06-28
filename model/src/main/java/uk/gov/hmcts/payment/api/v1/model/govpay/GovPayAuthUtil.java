package uk.gov.hmcts.payment.api.v1.model.govpay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.List;

@Component
public class GovPayAuthUtil {

    @Value("#{'${gov.pay.operational_services}'.split(',')}")
    private List<String> operationalServices;

    @Autowired
    private GovPayConfig govPayConfig;

    public String getServiceToken(String callingService, String paymentService) throws Exception {
        if (!callingService.equals(paymentService) && !operationalServices.contains(callingService)) {
            throw new AccessDeniedException("Unable to fetch information due to caller being forbidden to look up given payment");
        }

        return govPayConfig.getKey().get(paymentService);
    }
}
