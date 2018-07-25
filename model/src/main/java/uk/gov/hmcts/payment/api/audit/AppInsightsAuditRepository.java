package uk.gov.hmcts.payment.api.audit;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.Payment;

import java.util.Map;

@Component
public class AppInsightsAuditRepository implements AuditRepository {

    private final TelemetryClient telemetry;

    @Autowired
    public AppInsightsAuditRepository(@Value("${azure.application-insights.instrumentation-key}") String instrumentationKey,
                                      TelemetryClient telemetry) {
        TelemetryConfiguration.getActive().setInstrumentationKey(instrumentationKey);
        telemetry.getContext().getComponent().setVersion(getClass().getPackage().getImplementationVersion());
        this.telemetry = telemetry;
    }

    @Override
    public void trackEvent(String name, Map<String, String> properties) {
        telemetry.trackEvent(name, properties,null);
    }

    @Override
    public void trackPaymentEvent(String name, Payment payment) {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
            .put("paymentReference", payment.getReference())
            .put("amount", payment.getAmount().toString())
            .put("serviceType", payment.getServiceType())
            .put("status", payment.getStatus())
            .build();
        telemetry.trackEvent(name, properties,null);
    }
}
