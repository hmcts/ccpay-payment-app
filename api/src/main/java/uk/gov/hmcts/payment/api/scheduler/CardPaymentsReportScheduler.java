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
@ConditionalOnProperty("card.payments.report.scheduler.enabled")
public class CardPaymentsReportScheduler {

    private static final Logger LOG = getLogger(CardPaymentsReportScheduler.class);
    private PaymentsReportService paymentsReportService;

    @Value("${card.payments.report.startDate}")
    private String startDate;

    @Value("${card.payments.report.endDate}")
    private String endDate;

    private FeesRegisterClient feesRegisterClient;

    private Map<String,Fee2Dto> feesDataMap = Collections.emptyMap();

    @Autowired
    public CardPaymentsReportScheduler(PaymentsReportService paymentsReportService, FeesRegisterClient feesRegisterClient) {
        this.paymentsReportService= paymentsReportService;
        this.feesRegisterClient = feesRegisterClient;
    }

    @Scheduled(cron = "${card.payments.report.schedule}")
    public void generateCardPaymentsReportTask() {
        try {
            if (feesRegisterClient.getFeesDataAsMap().isPresent())
                feesDataMap = feesRegisterClient.getFeesDataAsMap().get();
        }
        catch(Exception ex){
            LOG.error("CardPaymentsReportScheduler - Unable to get fees data.");
        }
        LOG.info("CardPaymentsReportScheduler -  Start of scheduled job for HMCTS-Card Payments csv report file.");
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(startDate, endDate,feesDataMap);
        LOG.info("CardPaymentsReportScheduler -  End of scheduled job for HMCTS-Card Payments csv report file.");
    }

}
