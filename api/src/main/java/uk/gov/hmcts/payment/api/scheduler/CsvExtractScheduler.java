package uk.gov.hmcts.payment.api.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
@Component
public class CsvExtractScheduler {

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
        csvExtractService.extractCsv(startDate, endDate);
    }

}
