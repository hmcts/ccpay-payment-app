package uk.gov.hmcts.payment.api.scheduler;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.payment.api.scheduler.DateUtils.getTodayDate;
import static uk.gov.hmcts.payment.api.scheduler.DateUtils.getYesterdayDate;

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

    public void generateCreditAccountPaymentsReportTask() {

        try {

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            sdf.setLenient(false);

            Date fromDate = startDate == null ? getYesterdayDate() : sdf.parse(startDate);
            Date toDate = endDate == null ? getTodayDate() : sdf.parse(endDate);

            LOG.info("CreditAccountPaymentsReportScheduler -  Start of scheduled job for HMCTS-PBA Payments csv report file.");
            feesService.dailyRefreshOfFeesData();
            paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(fromDate, toDate);
            LOG.info("CreditAccountPaymentsReportScheduler -  End of scheduled job for HMCTS-PBA Payments csv report file.");

        } catch (ParseException paex) {

            LOG.error("CreditAccountPaymentsReportScheduler - Error while creating credit account payments csv file."
                + " Error message is " + paex.getMessage() + ". Expected format is dd-mm-yyyy.");

            throw new PaymentException("Input dates parsing exception, valid date format is dd-MM-yyyy");

        }

    }

}
