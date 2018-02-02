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
@ConditionalOnProperty("csv.extract.scheduler.enabled")
public class CsvExtractScheduler {

    private static final Logger LOG = getLogger(CsvExtractService.class);
    private CsvExtractService csvExtractService;

    private String startDate;

    private String endDate;

    @Autowired
    public CsvExtractScheduler(CsvExtractService csvExtractService, @Value("${payments.extract.startDate}") String startDate,
                               @Value("${payments.extract.endDate}") String endDate ) {
        this.csvExtractService= csvExtractService;
        this.startDate = startDate;
        this.endDate =endDate;

        }

    @Scheduled(cron = "${csv.extract.schedule}")
    public void extractCsv() throws ParseException, IOException {
        LOG.info("CsvExtractScheduler -  Start of nightly job for HMCTS-Payments csv extract file.");
        csvExtractService.extractCsv(startDate, endDate);
    }

}
