package uk.gov.hmcts.payment.api.scheduler;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.payment.api.fees.client.FeesRegisterClient;

import java.util.Collections;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty("pba.payments.report.scheduler.enabled")
public class CreditAccountPaymentsReportScheduler {

    private static final Logger LOG = getLogger(CreditAccountPaymentsReportScheduler.class);
    private PaymentsReportService paymentsReportService;

    @Value("${pba.payments.report.startDate}")
    private String startDate;

    @Value("${pba.payments.report.endDate}")
    private String endDate;

    private FeesRegisterClient feesRegisterClient;

    private Map<String, Fee2Dto> feesDataMap = Collections.emptyMap() ;


    @Autowired
    public CreditAccountPaymentsReportScheduler(PaymentsReportService paymentsReportService,FeesRegisterClient feesRegisterClient) {
        this.paymentsReportService = paymentsReportService;
        this.feesRegisterClient=feesRegisterClient;
    }

    @Scheduled(cron = "${pba.payments.report.schedule}")
    public void generateCreditAccountPaymentsReportTask() {
        try {
            if (feesRegisterClient.getFeesDataAsMap().isPresent())
                feesDataMap = feesRegisterClient.getFeesDataAsMap().get();
        } catch (Exception ex) {
            LOG.error("CreditAccountPaymentsReportSchedule - Unable to get fees data.");
        }
        LOG.info("CreditAccountPaymentsReportScheduler -  Start of scheduled job for HMCTS-PBA Payments csv report file.");
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(startDate, endDate,feesDataMap);
        LOG.info("CreditAccountPaymentsReportScheduler -  End of scheduled job for HMCTS-PBA Payments csv report file.");
    }

}
