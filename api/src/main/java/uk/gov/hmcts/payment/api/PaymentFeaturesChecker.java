package uk.gov.hmcts.payment.api;

import org.ff4j.FF4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;

import java.util.List;

@Component
public class PaymentFeaturesChecker {

    private final ServiceIdSupplier serviceIdSupplier;
    private final FF4j ff4j;
    private final List<String> accountStatusServices;

    @Autowired
    public PaymentFeaturesChecker(ServiceIdSupplier serviceIdSupplier, FF4j ff4j,
                                  @Value("#{'${liberata.status.check.required.services}'.split(',')}") List<String> accountStatusServices) {
        this.serviceIdSupplier = serviceIdSupplier;
        this.ff4j = ff4j;
        this.accountStatusServices = accountStatusServices;
    }

    public boolean isAccountStatusCheckRequired() {
        return ff4j.check("credit-account-payment-liberata-check") && accountStatusServices.contains(serviceIdSupplier.get());
    }
}
