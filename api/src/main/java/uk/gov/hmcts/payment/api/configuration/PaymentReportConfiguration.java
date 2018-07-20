package uk.gov.hmcts.payment.api.configuration;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;
import uk.gov.hmcts.payment.api.reports.config.CardPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaCmcPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaDivorcePaymentReportConfig;

import java.util.Map;

@Configuration
public class PaymentReportConfiguration {

    @Autowired private CardPaymentReportConfig cardPaymentReportConfig;
    @Autowired private PbaCmcPaymentReportConfig pbaCmcPaymentReportConfig;
    @Autowired private PbaDivorcePaymentReportConfig pbaDivorcePaymentReportConfig;

    @Bean
    public Map<PaymentReportType, PaymentReportConfig> configMap() {
        return ImmutableMap.<PaymentReportType, PaymentReportConfig>builder()
            .put(PaymentReportType.CARD, cardPaymentReportConfig)
            .put(PaymentReportType.PBA_CMC, pbaCmcPaymentReportConfig)
            .put(PaymentReportType.PBA_DIVORCE, pbaDivorcePaymentReportConfig)
            .build();

    }
}
