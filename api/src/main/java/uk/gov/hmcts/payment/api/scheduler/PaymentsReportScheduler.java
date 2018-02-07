package uk.gov.hmcts.payment.api.scheduler;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty("payments.report.scheduler.enabled")
public class PaymentsReportScheduler {

    private static final Logger LOG = getLogger(PaymentsReportService.class);
    private PaymentsReportService paymentsReportService;

    @Value("${payments.report.startDate}")
    private String startDate;

    @Value("${payments.report.endDate}")
    private String endDate;

    @Autowired
    public PaymentsReportScheduler(PaymentsReportService csvExtractService) {
        this.paymentsReportService= csvExtractService;
        }

    @Scheduled(cron = "${payments.report.schedule}")
    public void generatePaymentsReport() throws ParseException, IOException {
        LOG.info("PaymentsReportScheduler -  Start of scheduled job for HMCTS-Payments csv report file.");
        paymentsReportService.generateCsv(startDate, endDate);
        LOG.info("PaymentsReportScheduler -  End of scheduled job for HMCTS-Payments csv report file.");
    }

}
