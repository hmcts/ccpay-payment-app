package uk.gov.hmcts.payment.api.scheduler;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.io.FileUtils;
import org.joda.time.MutableDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.contract.CardPaymentDto;
import uk.gov.hmcts.payment.api.controllers.CardPaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.CardPaymentService;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class CsvExtractService {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

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

        createCsv(cardPayments);
    }

    private void createCsv(List<CardPaymentDto> cardPayments) throws IOException {

        FileUtils.cleanDirectory(new File(extractFileLocation));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String fileNameSuffix = LocalDateTime.now().format(formatter);
        String extractFileName = extractFileLocation+ "/extract_" + fileNameSuffix + ".csv";
        CsvMapper mapper = new CsvMapper();
        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        CsvSchema schema = CsvSchema.builder()
            .addColumn("payment_reference")
            .addColumn("ccd_case_number")
            .addColumn("case_reference")
            .addColumn("payment_date")
            .addColumn("payment_channel")
            .addColumn("amount")
            .addColumn("site_id")
            .setUseHeader( true )
            .build()
            .withLineSeparator("\r\n");

        ObjectWriter writer = mapper
            .writer(schema.withLineSeparator("\n"));
        writer.writeValue(new File(extractFileName), cardPayments);

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
