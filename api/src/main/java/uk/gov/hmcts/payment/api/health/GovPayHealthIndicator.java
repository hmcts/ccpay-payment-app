package uk.gov.hmcts.payment.api.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.external.client.GovPayClient;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;

@Component
public class GovPayHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovPayHealthIndicator.class);

    private final GovPayClient govPayClient;
    private final GovPayAuthUtil govPayAuthUtil;

    public GovPayHealthIndicator(GovPayClient govPayClient,
                                 GovPayAuthUtil govPayAuthUtil) {
        this.govPayClient = govPayClient;
        this.govPayAuthUtil = govPayAuthUtil;
    }

    private String keyForService(String service) {
        return govPayAuthUtil.getServiceToken(service);
    }

    @Override
    public Health health() {
        try {
            GovPayPayment govPayPayment = this.govPayClient.healthInfo(keyForService("cmc"));
            if(null != govPayPayment) {
                return statusHealthy();
            }else {
                return statusDown();
            }
        } catch (Exception ex) {
            LOGGER.error("Error on GovPay health check", ex);
            return statusUnknown(ex);
        }
    }

    private Health statusHealthy() {
        return Health.up().build();
    }

    private Health statusDown() {
        return Health.down().build();
    }

    private Health statusUnknown(Throwable ex) {
        return Health.unknown().withException(ex).build();
    }
}
