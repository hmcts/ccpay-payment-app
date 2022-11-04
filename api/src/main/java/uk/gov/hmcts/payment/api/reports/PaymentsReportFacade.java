package uk.gov.hmcts.payment.api.reports;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.reports.config.PaymentReportConfig;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.util.Date;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class PaymentsReportFacade {

    private static final Logger LOG = getLogger(PaymentsReportFacade.class);

    private final PaymentsReportService reportService;
    private final Map<PaymentReportType, PaymentReportConfig> configMap;

    @Autowired
    public PaymentsReportFacade(PaymentsReportService reportService, Map<PaymentReportType, PaymentReportConfig> configMap) {
        this.reportService = reportService;
        this.configMap = configMap;
    }

    public void generateCsvAndSendEmail(Date startDate, Date endDate, PaymentMethodType paymentMethodType, String serviceType) {
        LOG.info("Inside generateCsvAndSendEmail with paymentMethodType: {} and serviceType: {} and startDate: {} and endDate: {}"
            , paymentMethodType, serviceType, startDate, endDate);
        PaymentReportConfig reportConfig = configMap.get(PaymentReportType.from(paymentMethodType, serviceType));
        if (reportConfig.isEnabled()) {
            LOG.info("payments report flag is enabled for type :{} and service :{}. creating csv", paymentMethodType, serviceType);
            reportService.generateCsvAndSendEmail(startDate, endDate, paymentMethodType, serviceType, reportConfig);
        } else {
            LOG.info("payments report flag is disabled for type :{} and service :{}. So, system will not send CSV email", paymentMethodType, serviceType);
        }
    }
}
