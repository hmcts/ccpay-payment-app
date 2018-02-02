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
public class CsvExtractService {

    private static final Logger LOG = getLogger(CsvExtractService.class);

    private static final String EXTRACT_FILE_PREFIX="hmcts_payments_";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

    private static final String HEADER = "Service,Payment reference,CCD reference,Case reference,Payment date,Payment channel,Payment amount,"
        + "Site id,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version";

    private CardPaymentService<PaymentFeeLink, String> cardPaymentService;

    private CardPaymentDtoMapper cardPaymentDtoMapper;

    private String extractFileLocation;

    @Autowired
    public CsvExtractService(@Qualifier("loggingCardPaymentService") CardPaymentService<PaymentFeeLink, String> cardPaymentService, CardPaymentDtoMapper cardPaymentDtoMapper,
                             @Value("${csv.extract.file.location}") String extractFileLocation) {
        this.cardPaymentService = cardPaymentService;
        this.cardPaymentDtoMapper = cardPaymentDtoMapper;
        this.extractFileLocation = extractFileLocation;
    }

    public void extractCsv(String startDate, String endDate) throws ParseException, IOException {
        Date fromDate = startDate == null ? sdf.parse(getYesterdaysDate()) : sdf.parse(startDate);
        Date toDate = endDate == null ? sdf.parse(getTodaysDate()) : sdf.parse(endDate);

        // Limiting search only to date without time.
        MutableDateTime mutableToDate = new MutableDateTime(toDate);
        mutableToDate.addDays(1);

        List<CardPaymentDto> cardPayments = cardPaymentService.search(fromDate, mutableToDate.toDate()).stream()
            .map(cardPaymentDtoMapper::toReconciliationResponseDto).collect(Collectors.toList());

        LOG.info("CsvExtractScheduler - Total records found for CSV extract "+cardPayments.size()+".");

        createCsv(cardPayments);
    }

    private void createCsv(List<CardPaymentDto> cardPayments) throws IOException {

        File folder = new File(extractFileLocation);

        for (File file : folder.listFiles()) {
            if (file.getName().startsWith(EXTRACT_FILE_PREFIX) && file.getName().endsWith(".csv")) {
                file.delete();
            }
        }

        LOG.info("CsvExtractScheduler - Old extract files deleted ");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String fileNameSuffix = LocalDateTime.now().format(formatter);
        String extractFileName = extractFileLocation + File.separator + EXTRACT_FILE_PREFIX + fileNameSuffix + ".csv";

        Path path = Paths.get(extractFileName);

        try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.forName("UTF-8"))) {
            writer.write(HEADER);
            writer.newLine();
            for (CardPaymentDto cardPayment : cardPayments) {
                writer.write(cardPayment.toCsv());
                writer.newLine();
            }

            LOG.info("CsvExtractScheduler - "+ extractFileName +" file created.");

        } catch (IOException ex) {

            LOG.error("CsvExtractScheduler - Error while creating extract file "+ extractFileName + ". Error message is"+ex.getMessage());

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
