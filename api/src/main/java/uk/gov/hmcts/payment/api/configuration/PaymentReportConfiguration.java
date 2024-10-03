package uk.gov.hmcts.payment.api.configuration;

import com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;
import uk.gov.hmcts.payment.api.reports.config.*;

import java.util.Map;

@Configuration
public class PaymentReportConfiguration {

    @Bean
    public Map<PaymentReportType, PaymentReportConfig> configMap(DuplicatePaymentReportConfig duplicatePaymentReportConfig,
                                                                 CardPaymentReportConfig cardPaymentReportConfig,
                                                                 BarPaymentReportConfig barPaymentReportConfig,
                                                                 PbaCmcPaymentReportConfig pbaCmcPaymentReportConfig,
                                                                 PbaProbatePaymentReportConfig pbaProbatePaymentReportConfig,
                                                                 PbaFinremPaymentReportConfig pbaFinremPaymentReportConfig,
                                                                 PbaDivorcePaymentReportConfig pbaDivorcePaymentReportConfig,
                                                                 PbaFplPaymentReportConfig pbaFplPaymentReportConfig,
                                                                 PbaCivilPaymentReportConfig pbaCivilPaymentReportConfig,
                                                                 PbaPrlPaymentReportConfig pbaPrlPaymentReportConfig,
                                                                 PbaIacPaymentReportConfig pbaIacPaymentReportConfig,
                                                                 PbaSmcPaymentReportConfig pbaSmcPaymentReportConfig) {
        return ImmutableMap.<PaymentReportType, PaymentReportConfig>builder()
            .put(PaymentReportType.DUPLICATE_PAYMENT, duplicatePaymentReportConfig)
            .put(PaymentReportType.CARD, cardPaymentReportConfig)
            .put(PaymentReportType.DIGITAL_BAR, barPaymentReportConfig)
            .put(PaymentReportType.PBA_CMC, pbaCmcPaymentReportConfig)
            .put(PaymentReportType.PBA_PROBATE, pbaProbatePaymentReportConfig)
            .put(PaymentReportType.PBA_FINREM, pbaFinremPaymentReportConfig)
            .put(PaymentReportType.PBA_DIVORCE, pbaDivorcePaymentReportConfig)
            .put(PaymentReportType.PBA_FPL, pbaFplPaymentReportConfig)
            .put(PaymentReportType.PBA_CIVIL, pbaCivilPaymentReportConfig)
            .put(PaymentReportType.PBA_PRL, pbaPrlPaymentReportConfig)
            .put(PaymentReportType.PBA_IAC, pbaIacPaymentReportConfig)
            .put(PaymentReportType.PBA_SMC, pbaSmcPaymentReportConfig)
            .build();

    }
}
