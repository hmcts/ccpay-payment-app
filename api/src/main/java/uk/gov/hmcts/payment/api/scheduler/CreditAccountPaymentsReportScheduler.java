package uk.gov.hmcts.payment.api.scheduler;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;

import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class CreditAccountPaymentsReportScheduler {

    private static final Logger LOG = getLogger(CreditAccountPaymentsReportScheduler.class);
    private PaymentsReportService paymentsReportService;
    private FeesService feesService;

    @Value("${pba.payments.report.startDate}")
    private String startDate;

    @Value("${pba.payments.report.endDate}")
    private String endDate;

    @Autowired
    public CreditAccountPaymentsReportScheduler(PaymentsReportService paymentsReportService, FeesService feesService) {
        this.paymentsReportService = paymentsReportService;
        this.feesService = feesService;
    }

    public void generateCreditAccountPaymentsReportTask(Date fromDate, Date toDate) {
        LOG.info("CreditAccountPaymentsReportScheduler -  Start of scheduled job for HMCTS-PBA Payments csv report file.");
        feesService.dailyRefreshOfFeesData();
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(fromDate, toDate);
        LOG.info("CreditAccountPaymentsReportScheduler -  End of scheduled job for HMCTS-PBA Payments csv report file.");
    }

}
