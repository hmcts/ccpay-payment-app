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
@ConditionalOnProperty("pba.payments.report.scheduler.enabled")
public class CreditAccountPaymentsReportScheduler {

    private static final Logger LOG = getLogger(CreditAccountPaymentsReportScheduler.class);
    private PaymentsReportService paymentsReportService;

    @Value("${pba.payments.report.startDate:#{null}}")
    private String startDate;

    @Value("${pba.payments.report.endDate:#{null}}")
    private String endDate;

    @Autowired
    public CreditAccountPaymentsReportScheduler(PaymentsReportService csvExtractService) {
        this.paymentsReportService= csvExtractService;
    }

    @Scheduled(cron = "${pba.payments.report.schedule}")
    public void generateCreditAccountPaymentsReportTask()  {
        LOG.info("CreditAccountPaymentsReportScheduler -  Start of scheduled job for HMCTS-PBA Payments csv report file.");
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(startDate, endDate);
        LOG.info("CreditAccountPaymentsReportScheduler -  End of scheduled job for HMCTS-PBA Payments csv report file.");
    }

}
