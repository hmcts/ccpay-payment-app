package uk.gov.hmcts.payment.api.configuration;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.Map;

@Configuration
public class PaymentReportConfiguration {

    @Autowired
    private CardPaymentReportConfig cardPaymentReportConfig;
    @Autowired
    private BarPaymentReportConfig barPaymentReportConfig;
    @Autowired
    private PbaCmcPaymentReportConfig pbaCmcPaymentReportConfig;
    @Autowired
    private PbaProbatePaymentReportConfig pbaProbatePaymentReportConfig;
    @Autowired
    private PbaFinremPaymentReportConfig pbaFinremPaymentReportConfig;
    @Autowired
    private PbaDivorcePaymentReportConfig pbaDivorcePaymentReportConfig;

    @Bean
    public Map<PaymentReportType, PaymentReportConfig> configMap() {
        return ImmutableMap.<PaymentReportType, PaymentReportConfig>builder()
            .put(PaymentReportType.CARD, cardPaymentReportConfig)
            .put(PaymentReportType.DIGITAL_BAR, barPaymentReportConfig)
            .put(PaymentReportType.PBA_CMC, pbaCmcPaymentReportConfig)
            .put(PaymentReportType.PBA_PROBATE, pbaProbatePaymentReportConfig)
            .put(PaymentReportType.PBA_FINREM, pbaFinremPaymentReportConfig)
            .put(PaymentReportType.PBA_DIVORCE, pbaDivorcePaymentReportConfig)
            .build();

    }
}
