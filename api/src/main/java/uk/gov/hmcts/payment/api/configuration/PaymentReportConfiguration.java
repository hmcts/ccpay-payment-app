package uk.gov.hmcts.payment.api.configuration;

import com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;
import uk.gov.hmcts.payment.api.reports.config.BarPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.CardPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaCmcPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaDivorcePaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaFinremPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaProbatePaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaFplPaymentReportConfig;

import java.util.Map;

@Configuration
public class PaymentReportConfiguration {

    @Bean
    public Map<PaymentReportType, PaymentReportConfig> configMap(CardPaymentReportConfig cardPaymentReportConfig,
                                                                 BarPaymentReportConfig barPaymentReportConfig,
                                                                 PbaCmcPaymentReportConfig pbaCmcPaymentReportConfig,
                                                                 PbaProbatePaymentReportConfig pbaProbatePaymentReportConfig,
                                                                 PbaFinremPaymentReportConfig pbaFinremPaymentReportConfig,
                                                                 PbaDivorcePaymentReportConfig pbaDivorcePaymentReportConfig,
                                                                 PbaFplPaymentReportConfig pbaFplPaymentReportConfig) {
        return ImmutableMap.<PaymentReportType, PaymentReportConfig>builder()
            .put(PaymentReportType.CARD, cardPaymentReportConfig)
            .put(PaymentReportType.DIGITAL_BAR, barPaymentReportConfig)
            .put(PaymentReportType.PBA_CMC, pbaCmcPaymentReportConfig)
            .put(PaymentReportType.PBA_PROBATE, pbaProbatePaymentReportConfig)
            .put(PaymentReportType.PBA_FINREM, pbaFinremPaymentReportConfig)
            .put(PaymentReportType.PBA_DIVORCE, pbaDivorcePaymentReportConfig)
            .put(PaymentReportType.PBA_FPL, pbaFplPaymentReportConfig)
            .build();

    }
}
