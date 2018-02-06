package uk.gov.hmcts.payment.api.scheduler;

import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.contract.CardPaymentDto;
import uk.gov.hmcts.payment.api.controllers.CardPaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.CardPaymentService;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class PaymentsReportService {

    private static final Logger LOG = getLogger(PaymentsReportService.class);

    private static final String CSV_FILE_PREFIX = "hmcts_payments_";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

    private static final String HEADER = "Service,Payment reference,CCD reference,Case reference,Payment date,Payment channel,Payment amount,"
        + "Site id,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version";

    private CardPaymentService<PaymentFeeLink, String> cardPaymentService;

    private CardPaymentDtoMapper cardPaymentDtoMapper;

    private String csvFileLocation;

    @Autowired
    public PaymentsReportService(@Qualifier("loggingCardPaymentService") CardPaymentService<PaymentFeeLink, String> cardPaymentService, CardPaymentDtoMapper cardPaymentDtoMapper,
                                 @Value("${payments.report.file.location}") String csvFileLocation) {
        this.cardPaymentService = cardPaymentService;
        this.cardPaymentDtoMapper = cardPaymentDtoMapper;
        this.csvFileLocation = csvFileLocation;
    }

    public void generateCsv(String startDate, String endDate) throws ParseException, IOException {

        Date fromDate = startDate == null ? sdf.parse(getYesterdaysDate()) : sdf.parse(startDate);
        Date toDate = endDate == null ? sdf.parse(getTodaysDate()) : sdf.parse(endDate);

        // Limiting search only to date without time.
        MutableDateTime mutableToDate = new MutableDateTime(toDate);
        mutableToDate.addDays(1);

        List<CardPaymentDto> cardPayments = cardPaymentService.search(fromDate, mutableToDate.toDate()).stream()
            .map(cardPaymentDtoMapper::toReconciliationResponseDto).collect(Collectors.toList());

        createCsv(cardPayments);

    }

    private void createCsv(List<CardPaymentDto> cardPayments) throws IOException {

        File folder = new File(csvFileLocation);

        for (File file : folder.listFiles()) {
            if (file.getName().startsWith(CSV_FILE_PREFIX) && file.getName().endsWith(".csv")) {
                file.delete();
                LOG.info("PaymentsReportService - " + file.getName() + " deleted ");
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String fileNameSuffix = LocalDateTime.now().format(formatter);
        String csvFileName = csvFileLocation + File.separator + CSV_FILE_PREFIX + fileNameSuffix + ".csv";

        Path path = Paths.get(csvFileName);

        try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.forName("UTF-8"))) {
            writer.write(HEADER);
            writer.newLine();
            for (CardPaymentDto cardPayment : cardPayments) {
                writer.write(cardPayment.toCsv());
                writer.newLine();
            }

            LOG.info("PaymentsReportService - Total " + cardPayments.size() + " records written in payments csv file " + csvFileName);

        } catch (IOException ex) {

            LOG.error("PaymentsReportService - Error while creating extract file " + csvFileName + ". Error message is" + ex.getMessage());

        }


    }

    private String getYesterdaysDate() {
        Date now = new Date();
        MutableDateTime mtDtNow = new MutableDateTime(now);
        mtDtNow.addDays(-1);
        return sdf.format(mtDtNow.toDate());
    }

    private String getTodaysDate() {
        return sdf.format(new Date());
    }

}
