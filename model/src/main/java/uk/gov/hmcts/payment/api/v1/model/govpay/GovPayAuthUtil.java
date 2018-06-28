package uk.gov.hmcts.payment.api.v1.model.govpay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentInformationForbidden;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class GovPayAuthUtil {

    @Value("${gov.pay.operational_services}")
    private String[] operationalServices;

    private List<String> operationalServicesList;

    @Autowired
    private GovPayConfig govPayConfig;

    public GovPayAuthUtil() {
        operationalServicesList = Arrays.asList(operationalServices);
    }

    public String getServiceToken(String callingService, String paymentService) throws Exception {
        if (!callingService.equals(paymentService) && !operationalServicesList.contains(callingService)) {
            throw new PaymentInformationForbidden("Unable to fetch information due to caller being forbidden to look up given payment");
        }

        return govPayConfig.getKey().get(paymentService);
    }
}
