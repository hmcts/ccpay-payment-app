package uk.gov.hmcts.payment.api.scheduler;


import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.payment.api.fees.client.FeesRegisterClient;

import java.util.Collections;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "manual.csv.report.generation", havingValue = "true")
public class GenerateCsvReportsAtApplicationStartUp implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOG = getLogger(GenerateCsvReportsAtApplicationStartUp.class);

    @Value("${card.payments.report.startDate}")
    private String cardPaymentsStartDate;

    @Value("${card.payments.report.endDate}")
    private String cardPaymentsEndDate;

    @Value("${pba.payments.report.startDate}")
    private String creditAccountPaymentsStartDate;

    @Value("${pba.payments.report.endDate}")
    private String creditAccountPaymentsEndDate;


    private PaymentsReportService paymentsReportService;

    private FeesRegisterClient feesRegisterClient;

    private Map<String, Fee2Dto> feesDataMap = Collections.emptyMap();


    @Autowired
    public GenerateCsvReportsAtApplicationStartUp(PaymentsReportService paymentsReportService, FeesRegisterClient feesRegisterClient) {
        this.paymentsReportService = paymentsReportService;
        this.feesRegisterClient = feesRegisterClient;
    }


    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        try {
              if (feesRegisterClient.getFeesDataAsMap().isPresent())
                  feesDataMap = feesRegisterClient.getFeesDataAsMap().get();

        } catch (Exception ex) {
            LOG.info("GenerateCsvReportsAtApplicationStartUp - Unable to get fees data");

        }

        LOG.info("GenerateCsvReportsAtApplicationStartUp -  Start of generation of HMCTS-Card Payments csv report file.");
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(cardPaymentsStartDate, cardPaymentsEndDate, feesDataMap);
        LOG.info("GenerateCsvReportsAtApplicationStartUp -  End of generation of HMCTS-Card Payments csv report file.");

        LOG.info("GenerateCsvReportsAtApplicationStartUp -  Start of generation of HMCTS-PBA Payments csv report file.");
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(creditAccountPaymentsStartDate, creditAccountPaymentsEndDate, feesDataMap);
        LOG.info("GenerateCsvReportsAtApplicationStartUp -  End of generation of HMCTS-PBA Payments csv report file.");

    }
}
